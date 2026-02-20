from __future__ import annotations

import argparse
from dataclasses import dataclass
from statistics import mean, median, stdev
from typing import Dict, List, Tuple

import numpy as np

from src.service.simulator_service import (
    BUYER_PERSONAS,
    BUYER_WEIGHTS,
    LOCK_FAIL_P,
    NEXT_BEST_PRICE_MULTIPLIER,
)


@dataclass
class VariantMetrics:
    attempts: int = 0
    lock_failed: int = 0
    next_best_offered: int = 0
    next_best_accepted: int = 0
    checkout_start: int = 0
    checkout_abandoned: int = 0
    payment_completed: int = 0
    payment_failed: int = 0
    price_deltas: List[int] = None

    def __post_init__(self) -> None:
        if self.price_deltas is None:
            self.price_deltas = []

    def buy_now_cvr(self) -> float:
        return pct(self.payment_completed, self.attempts)

    def lock_fail_pct(self) -> float:
        return pct(self.lock_failed, self.attempts)

    def checkout_fail_pct(self) -> float:
        return pct(self.payment_failed, self.checkout_start)

    def next_best_accept_pct(self) -> float:
        return pct(self.next_best_accepted, self.next_best_offered)


def pct(n: int, d: int) -> float:
    if d <= 0:
        return 0.0
    return round(n / d * 100, 2)


def weighted_choice(rng: np.random.Generator, weights: List[float]) -> int:
    total = sum(weights)
    if total <= 0:
        return 0
    r = rng.random()
    cumulative = 0.0
    for idx, w in enumerate(weights):
        cumulative += w / total
        if r <= cumulative:
            return idx
    return len(weights) - 1


def simulate_paired(
    seed: int,
    users: int,
    days: int,
    avg_sessions_per_user: float,
    seller_ratio: float,
) -> Tuple[VariantMetrics, VariantMetrics]:
    rng = np.random.default_rng(seed)
    seller_ratio = min(max(seller_ratio, 0.0), 1.0)

    a = VariantMetrics()
    b = VariantMetrics()

    for _ in range(users):
        is_seller = rng.random() < seller_ratio
        if is_seller:
            continue

        persona_idx = weighted_choice(rng, BUYER_WEIGHTS)
        persona = BUYER_PERSONAS[persona_idx]

        for _ in range(days):
            sessions = rng.poisson(lam=avg_sessions_per_user)
            if sessions <= 0:
                continue

            for _ in range(int(sessions)):
                base_price = int(rng.integers(70000, 650001))

                buy_now_roll = rng.random()
                if buy_now_roll > persona.buy_now_p:
                    continue

                a.attempts += 1
                b.attempts += 1

                lock_fail_roll = rng.random()
                lock_failed = lock_fail_roll <= LOCK_FAIL_P
                if lock_failed:
                    a.lock_failed += 1
                    b.lock_failed += 1

                    b.next_best_offered += 1
                    next_best_price = int(base_price * rng.uniform(*NEXT_BEST_PRICE_MULTIPLIER))
                    b.price_deltas.append(next_best_price - base_price)

                    accept_roll = rng.random()
                    if accept_roll <= persona.next_best_accept_p:
                        b.next_best_accepted += 1
                        b.checkout_start += 1
                        pay_roll = rng.random()
                        if pay_roll <= persona.payment_success_p:
                            b.payment_completed += 1
                        else:
                            b.payment_failed += 1
                        continue

                    b.checkout_abandoned += 1
                    continue

                # lock success -> checkout for both A/B
                a.checkout_start += 1
                b.checkout_start += 1
                pay_roll = rng.random()
                if pay_roll <= persona.payment_success_p:
                    a.payment_completed += 1
                    b.payment_completed += 1
                else:
                    a.payment_failed += 1
                    b.payment_failed += 1

    return a, b


def summarize(a: VariantMetrics, b: VariantMetrics) -> Dict[str, float]:
    return {
        "a_buy_now_cvr": a.buy_now_cvr(),
        "b_buy_now_cvr": b.buy_now_cvr(),
        "delta_buy_now_cvr": round(b.buy_now_cvr() - a.buy_now_cvr(), 2),
        "a_lock_fail_pct": a.lock_fail_pct(),
        "b_lock_fail_pct": b.lock_fail_pct(),
        "a_checkout_fail_pct": a.checkout_fail_pct(),
        "b_checkout_fail_pct": b.checkout_fail_pct(),
        "b_next_best_accept_pct": b.next_best_accept_pct(),
        "b_price_delta_avg": round(mean(b.price_deltas), 2) if b.price_deltas else 0.0,
        "b_price_delta_median": round(median(b.price_deltas), 2) if b.price_deltas else 0.0,
    }


def print_summary(title: str, metrics: Dict[str, float]) -> None:
    print(f"\n{title}")
    print("-" * len(title))
    for k, v in metrics.items():
        print(f"{k}: {v}")


def print_stats(title: str, summaries: List[Dict[str, float]]) -> None:
    print(f"\n{title}")
    print("-" * len(title))
    n = len(summaries)
    if n == 0:
        print("no data")
        return

    keys = summaries[0].keys()
    for key in keys:
        values = [s[key] for s in summaries]
        avg = mean(values)
        if n >= 2:
            sd = stdev(values)
            se = sd / (n ** 0.5)
            ci_low = avg - 1.96 * se
            ci_high = avg + 1.96 * se
            print(
                f"{key}: mean={avg:.2f} std={sd:.2f} 95%CI=[{ci_low:.2f}, {ci_high:.2f}]"
            )
        else:
            print(f"{key}: mean={avg:.2f} std=0.00 95%CI=[{avg:.2f}, {avg:.2f}]")


def main() -> None:
    parser = argparse.ArgumentParser(description="Paired A/B simulation (buy_now flow)")
    parser.add_argument("--users", type=int, default=30000)
    parser.add_argument("--days", type=int, default=14)
    parser.add_argument("--avg-sessions", type=float, default=1.2)
    parser.add_argument("--seller-ratio", type=float, default=0.35)
    parser.add_argument("--seed", type=int, default=48)
    parser.add_argument("--runs", type=int, default=1)
    parser.add_argument("--seed-start", type=int, default=None)
    args = parser.parse_args()

    seeds = []
    if args.runs <= 1:
        seeds = [args.seed]
    else:
        start = args.seed_start if args.seed_start is not None else args.seed
        seeds = list(range(start, start + args.runs))

    summaries = []
    for seed in seeds:
        a, b = simulate_paired(
            seed=seed,
            users=args.users,
            days=args.days,
            avg_sessions_per_user=args.avg_sessions,
            seller_ratio=args.seller_ratio,
        )
        summary = summarize(a, b)
        print_summary(f"Seed {seed}", summary)
        summaries.append(summary)

    if len(summaries) > 1:
        avg_summary = {
            key: round(mean(s[key] for s in summaries), 2)
            for key in summaries[0].keys()
        }
        print_summary("Average over seeds", avg_summary)
        print_stats("Std Dev & 95% CI", summaries)


if __name__ == "__main__":
    main()
