import math
import random
import threading
import time
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import parse_qs, urlparse


random.seed(7)

DEFAULT_STATE = {
    "start": 0.0,
    "req_total": 0,
    "req_2xx": 0,
    "req_4xx": 0,
    "req_5xx": 0,
    "repo_total": 0,
    "repo_sum": 0.0,
    "kafka_send": 0,
    "kafka_listener": 0,
    "jvm_heap": 450_000_000,
    "threads": 90,
    "cpu_usage": 0.35,
    "tomcat_busy": 20,
    "tomcat_max": 200,
    "hikari_active": 8,
    "hikari_pending": 1,
    "hikari_max": 30,
    "kafka_lag_order": 30,
    "kafka_lag_payment": 20,
    "kafka_lag_trade": 15,
    "hpa_desired": 2,
    "hpa_current": 2,
    "outbox_pending": 8,
    "outbox_published": 0,
    "outbox_failed": 0,
    "outbox_delay": 0.3,
}

STATE_BY_SERVICE = {}

LOCK = threading.Lock()


def get_state(service):
    state = STATE_BY_SERVICE.get(service)
    if state is None:
        state = DEFAULT_STATE.copy()
        state["start"] = time.time()
        STATE_BY_SERVICE[service] = state
    return state


def advance_state(state, service):
    now = time.time()

    if service == "product":
        spike = 1.0 + 0.6 * max(0.0, math.sin((now - state["start"]) / 6))
        base = int(random.randint(220, 320) * spike)
        err4 = random.randint(1, 8)
        err5 = random.randint(0, 4)
        repo = random.randint(90, 180)
    else:
        base = random.randint(120, 220)
        err4 = random.randint(0, 5)
        err5 = random.randint(0, 3)
        repo = random.randint(60, 140)

    ok = max(0, base - err4 - err5)

    state["req_total"] += base
    state["req_2xx"] += ok
    state["req_4xx"] += err4
    state["req_5xx"] += err5

    state["repo_total"] += repo
    state["repo_sum"] += repo * random.uniform(0.02, 0.08)

    state["kafka_send"] += random.randint(80, 160)
    state["kafka_listener"] += random.randint(70, 150)

    t = now - state["start"]
    state["jvm_heap"] = int(400_000_000 + 120_000_000 * (1 + math.sin(t / 9)) / 2)
    state["threads"] = int(90 + 12 * math.sin(t / 6))

    if service == "product":
        cpu = 0.65 + 0.05 * math.sin(t / 7) + random.uniform(-0.03, 0.03)
        state["cpu_usage"] = max(0.55, min(0.78, cpu))
        desired = 2
        if state["cpu_usage"] > 0.60:
            desired = 3
        if state["cpu_usage"] > 0.65:
            desired = 4
        if state["cpu_usage"] > 0.70:
            desired = 5
        if state["cpu_usage"] > 0.75:
            desired = 6
        state["hpa_desired"] = desired
        if state["hpa_current"] < desired:
            state["hpa_current"] += 1
        elif state["hpa_current"] > desired:
            state["hpa_current"] -= 1
    else:
        cpu = 0.35 + 0.08 * math.sin(t / 6) + random.uniform(-0.03, 0.03)
        state["cpu_usage"] = max(0.15, min(0.55, cpu))
        state["hpa_desired"] = 2
        state["hpa_current"] = min(state["hpa_current"], 2)

    if service == "payment":
        new_events = int(base * 0.25) + random.randint(5, 15)
        processed = int(new_events * random.uniform(0.6, 0.9))
        failures = int(processed * random.uniform(0.01, 0.03))
        published = max(0, processed - failures)

        state["outbox_pending"] = max(0, state["outbox_pending"] + new_events - processed)
        state["outbox_published"] += published
        state["outbox_failed"] += failures

        delay = 0.25 + (state["outbox_pending"] / 40.0) + random.uniform(-0.05, 0.08)
        state["outbox_delay"] = max(0.1, min(4.0, delay))

    state["tomcat_busy"] = int(20 + 8 * math.sin(t / 5))
    state["tomcat_max"] = 200
    state["hikari_active"] = int(8 + 4 * math.sin(t / 7))
    state["hikari_pending"] = max(0, int(2 + 2 * math.sin(t / 8)))
    state["hikari_max"] = 30

    state["kafka_lag_order"] = max(0, int(30 + 25 * math.sin(t / 4)))
    state["kafka_lag_payment"] = max(0, int(20 + 15 * math.sin(t / 5)))
    state["kafka_lag_trade"] = max(0, int(15 + 10 * math.sin(t / 6)))


def render_metrics(state, service):
    total = state["req_total"]
    repo_total = state["repo_total"]

    http_buckets = [
        (0.05, 0.50),
        (0.1, 0.80),
        (0.2, 0.93),
        (0.5, 0.98),
        (1.0, 0.995),
        (2.0, 0.999),
        (5.0, 1.0),
        (float("inf"), 1.0),
    ]

    repo_buckets = [
        (0.005, 0.40),
        (0.01, 0.70),
        (0.02, 0.88),
        (0.05, 0.95),
        (0.1, 0.98),
        (0.2, 0.995),
        (0.5, 1.0),
        (float("inf"), 1.0),
    ]

    lines = []

    lines.append("# HELP http_server_requests_seconds_count Total HTTP requests")
    lines.append("# TYPE http_server_requests_seconds_count counter")
    lines.append(f'http_server_requests_seconds_count{{status="200"}} {state["req_2xx"]}')
    lines.append(f'http_server_requests_seconds_count{{status="404"}} {state["req_4xx"]}')
    lines.append(f'http_server_requests_seconds_count{{status="500"}} {state["req_5xx"]}')

    lines.append("# HELP http_server_requests_seconds_bucket HTTP request duration buckets")
    lines.append("# TYPE http_server_requests_seconds_bucket counter")
    for le, pct in http_buckets:
        count = int(total * pct)
        le_value = "+Inf" if math.isinf(le) else f"{le:g}"
        lines.append(f'http_server_requests_seconds_bucket{{le="{le_value}"}} {count}')

    lines.append("# HELP http_server_requests_seconds_sum Total HTTP request duration")
    lines.append("# TYPE http_server_requests_seconds_sum counter")
    lines.append(f"http_server_requests_seconds_sum {total * 0.18:.3f}")

    lines.append("# HELP jvm_memory_used_bytes JVM memory used")
    lines.append("# TYPE jvm_memory_used_bytes gauge")
    lines.append(f'jvm_memory_used_bytes{{area="heap"}} {state["jvm_heap"]}')

    lines.append("# HELP jvm_threads_live_threads Live JVM threads")
    lines.append("# TYPE jvm_threads_live_threads gauge")
    lines.append(f"jvm_threads_live_threads {state['threads']}")

    lines.append("# HELP process_cpu_usage Process CPU usage")
    lines.append("# TYPE process_cpu_usage gauge")
    lines.append(f"process_cpu_usage {state['cpu_usage']:.3f}")

    if service == "product":
        lines.append("# HELP kube_hpa_status_current_replicas Current replicas (simulated)")
        lines.append("# TYPE kube_hpa_status_current_replicas gauge")
        lines.append(
            f'kube_hpa_status_current_replicas{{hpa="product-hpa"}} {state["hpa_current"]}'
        )
        lines.append("# HELP kube_hpa_status_desired_replicas Desired replicas (simulated)")
        lines.append("# TYPE kube_hpa_status_desired_replicas gauge")
        lines.append(
            f'kube_hpa_status_desired_replicas{{hpa="product-hpa"}} {state["hpa_desired"]}'
        )

    if service == "payment":
        lines.append("# HELP outbox_pending_messages Pending outbox messages")
        lines.append("# TYPE outbox_pending_messages gauge")
        lines.append(f"outbox_pending_messages {state['outbox_pending']}")

        lines.append("# HELP outbox_processing_delay_seconds Outbox processing delay")
        lines.append("# TYPE outbox_processing_delay_seconds gauge")
        lines.append(f"outbox_processing_delay_seconds {state['outbox_delay']:.3f}")

        lines.append("# HELP outbox_messages_published_total Published outbox messages")
        lines.append("# TYPE outbox_messages_published_total counter")
        lines.append(f"outbox_messages_published_total {state['outbox_published']}")

        lines.append("# HELP outbox_messages_failed_total Failed outbox messages")
        lines.append("# TYPE outbox_messages_failed_total counter")
        lines.append(f"outbox_messages_failed_total {state['outbox_failed']}")

    lines.append("# HELP tomcat_threads_busy_threads Busy Tomcat threads")
    lines.append("# TYPE tomcat_threads_busy_threads gauge")
    lines.append(f"tomcat_threads_busy_threads {state['tomcat_busy']}")

    lines.append("# HELP tomcat_threads_config_max_threads Max Tomcat threads")
    lines.append("# TYPE tomcat_threads_config_max_threads gauge")
    lines.append(f"tomcat_threads_config_max_threads {state['tomcat_max']}")

    lines.append("# HELP hikaricp_connections_active Active Hikari connections")
    lines.append("# TYPE hikaricp_connections_active gauge")
    lines.append(f"hikaricp_connections_active {state['hikari_active']}")

    lines.append("# HELP hikaricp_connections_pending Pending Hikari connections")
    lines.append("# TYPE hikaricp_connections_pending gauge")
    lines.append(f"hikaricp_connections_pending {state['hikari_pending']}")

    lines.append("# HELP hikaricp_connections_max Max Hikari connections")
    lines.append("# TYPE hikaricp_connections_max gauge")
    lines.append(f"hikaricp_connections_max {state['hikari_max']}")

    lines.append("# HELP spring_kafka_listener_seconds_count Kafka listener calls")
    lines.append("# TYPE spring_kafka_listener_seconds_count counter")
    lines.append(f"spring_kafka_listener_seconds_count {state['kafka_listener']}")

    lines.append("# HELP kafka_producer_record_send_total Kafka producer sends")
    lines.append("# TYPE kafka_producer_record_send_total counter")
    lines.append(f"kafka_producer_record_send_total {state['kafka_send']}")

    lines.append("# HELP kafka_consumer_fetch_manager_records_lag Kafka consumer lag")
    lines.append("# TYPE kafka_consumer_fetch_manager_records_lag gauge")
    lines.append(
        f'kafka_consumer_fetch_manager_records_lag{{topic="order-events"}} {state["kafka_lag_order"]}'
    )
    lines.append(
        f'kafka_consumer_fetch_manager_records_lag{{topic="payment-events"}} {state["kafka_lag_payment"]}'
    )
    lines.append(
        f'kafka_consumer_fetch_manager_records_lag{{topic="trade-events"}} {state["kafka_lag_trade"]}'
    )

    lines.append(
        "# HELP spring_data_repository_invocations_seconds_bucket Spring Data repository duration buckets"
    )
    lines.append("# TYPE spring_data_repository_invocations_seconds_bucket counter")
    for le, pct in repo_buckets:
        count = int(repo_total * pct)
        le_value = "+Inf" if math.isinf(le) else f"{le:g}"
        lines.append(f'spring_data_repository_invocations_seconds_bucket{{le="{le_value}"}} {count}')

    lines.append(
        "# HELP spring_data_repository_invocations_seconds_count Spring Data repository calls"
    )
    lines.append("# TYPE spring_data_repository_invocations_seconds_count counter")
    lines.append(f"spring_data_repository_invocations_seconds_count {repo_total}")

    lines.append(
        "# HELP spring_data_repository_invocations_seconds_sum Spring Data repository call duration"
    )
    lines.append("# TYPE spring_data_repository_invocations_seconds_sum counter")
    lines.append(f"spring_data_repository_invocations_seconds_sum {state['repo_sum']:.3f}")

    return "\n".join(lines) + "\n"


class MetricsHandler(BaseHTTPRequestHandler):
    def do_GET(self):
        parsed = urlparse(self.path)
        if parsed.path != "/metrics":
            self.send_response(200)
            self.send_header("Content-Type", "text/plain; charset=utf-8")
            self.end_headers()
            self.wfile.write(b"OK\n")
            return

        with LOCK:
            params = parse_qs(parsed.query)
            service = (params.get("service", ["default"])[0] or "default").lower()
            state = get_state(service)
            advance_state(state, service)
            body = render_metrics(state, service)

        self.send_response(200)
        self.send_header("Content-Type", "text/plain; version=0.0.4")
        self.end_headers()
        self.wfile.write(body.encode("utf-8"))

    def log_message(self, fmt, *args):
        return


def main():
    server = ThreadingHTTPServer(("0.0.0.0", 8080), MetricsHandler)
    server.serve_forever()


if __name__ == "__main__":
    main()
