from __future__ import annotations

import hashlib
import random
import uuid
from dataclasses import dataclass
from datetime import date, datetime, time, timedelta, timezone
from typing import Dict, List, Optional, Tuple

import numpy as np
from faker import Faker

from src.dto.simulator_dto import SimulatorRunRequest, SimulatorRunResponse
from src.models.analytics_event import AnalyticsEvent
from src.repository.event_repo import AnalyticsEventRepository


@dataclass
class BuyerProfile:
    name: str
    wishlist_p: float
    bid_p: float
    buy_now_p: float
    next_best_accept_p: float
    match_to_checkout_p: float
    payment_success_p: float


@dataclass
class SellerProfile:
    name: str
    match_p: float
    complete_p: float


BUYER_PERSONAS: List[BuyerProfile] = [
    BuyerProfile(
        "browser",
        wishlist_p=0.20,
        bid_p=0.08,
        buy_now_p=0.05,
        next_best_accept_p=0.25,
        match_to_checkout_p=0.50,
        payment_success_p=0.90,
    ),
    BuyerProfile(
        "price_sensitive",
        wishlist_p=0.45,
        bid_p=0.40,
        buy_now_p=0.08,
        next_best_accept_p=0.30,
        match_to_checkout_p=0.60,
        payment_success_p=0.92,
    ),
    BuyerProfile(
        "impulse",
        wishlist_p=0.10,
        bid_p=0.12,
        buy_now_p=0.20,
        next_best_accept_p=0.65,
        match_to_checkout_p=0.80,
        payment_success_p=0.95,
    ),
]

BUYER_WEIGHTS = [0.5, 0.3, 0.2]

SELLER_PERSONAS: List[SellerProfile] = [
    SellerProfile("casual_seller", match_p=0.25, complete_p=0.85),
    SellerProfile("reseller", match_p=0.45, complete_p=0.95),
]

SELLER_WEIGHTS = [0.6, 0.4]

EVENT_NAMES = [
    "view_item",
    "add_to_wishlist",
    "buy_now_attempt",
    "lock_failed",
    "next_best_offered",
    "next_best_accepted",
    "place_bid",
    "match_bid",
    "checkout_start",
    "checkout_abandoned",
    "payment_completed",
    "payment_failed",
    "bid_expired",
    "list_item",
    "set_ask",
    "sell_completed",
]

CATEGORIES = ["sneakers", "apparel", "accessories", "electronics", "collectibles"]
DEVICES = ["mobile", "desktop"]
REFERRERS = ["search", "home", "recommendation", "notification", "external"]
BRANDS = ["Nike", "Adidas", "New Balance", "ASICS", "Jordan", "Apple", "Sony", "LEGO"]
SIZES = ["230", "240", "250", "260", "270", "280", "290"]
CONDITIONS = ["new", "like_new", "used"]
CHECKOUT_ABANDON_REASONS = ["payment_method_missing", "verification_delay", "time_limit"]
PAYMENT_FAIL_REASONS = ["card_declined", "limit_exceeded", "pg_error", "3ds_failed"]
NEXT_BEST_DECLINE_REASONS = ["price_increase", "alternative_found", "changed_mind"]
LOCK_FAIL_P = 0.22
NEXT_BEST_PRICE_MULTIPLIER = (1.02, 1.08)


class SimulatorService:
    def __init__(self, event_repo: AnalyticsEventRepository):
        self.event_repo = event_repo
        self.faker = Faker("ko_KR")

    def run(self, req: SimulatorRunRequest) -> SimulatorRunResponse:
        if req.seed is not None:
            random.seed(req.seed)
            np.random.seed(req.seed)
            self.faker.seed_instance(req.seed)

        started_at = datetime.now(timezone.utc)
        start_date = req.start_date or (date.today() - timedelta(days=req.days - 1))
        user_ids = [uuid.uuid4() for _ in range(req.users)]

        variant_map = self._assign_variants(
            user_ids,
            req.experiment_id,
            req.variant_splits,
        )
        seller_ratio = min(max(req.seller_ratio, 0.0), 1.0)
        role_map = {
            uid: ("seller" if random.random() < seller_ratio else "buyer")
            for uid in user_ids
        }
        buyer_persona_map = {
            uid: random.choices(BUYER_PERSONAS, weights=BUYER_WEIGHTS, k=1)[0]
            for uid in user_ids
            if role_map[uid] == "buyer"
        }
        seller_persona_map = {
            uid: random.choices(SELLER_PERSONAS, weights=SELLER_WEIGHTS, k=1)[0]
            for uid in user_ids
            if role_map[uid] == "seller"
        }

        total_events = 0
        total_sessions = 0
        by_event: Dict[str, int] = {name: 0 for name in EVENT_NAMES}

        for day_offset in range(req.days):
            day = start_date + timedelta(days=day_offset)
            events_batch: List[AnalyticsEvent] = []

            for uid in user_ids:
                session_count = np.random.poisson(lam=req.avg_sessions_per_user)
                if session_count <= 0:
                    continue

                total_sessions += int(session_count)
                role = role_map[uid]
                variant_id = variant_map.get(uid)

                for _ in range(int(session_count)):
                    session_id = str(uuid.uuid4())
                    product_id = str(uuid.uuid4())
                    category = random.choice(CATEGORIES)
                    brand = random.choice(BRANDS)
                    size = random.choice(SIZES)
                    condition = random.choice(CONDITIONS)
                    device = random.choice(DEVICES)
                    referrer = random.choice(REFERRERS)
                    base_price = random.randint(70000, 650000)

                    ts = self._random_time_on_day(day)
                    if role == "buyer":
                        persona = buyer_persona_map[uid]
                        events, counts = self._build_buyer_events(
                            uid=uid,
                            session_id=session_id,
                            persona=persona,
                            product_id=product_id,
                            category=category,
                            brand=brand,
                            size=size,
                            condition=condition,
                            price=base_price,
                            device=device,
                            referrer=referrer,
                            occurred_at=ts,
                            experiment_id=req.experiment_id,
                            variant_id=variant_id,
                        )
                    else:
                        persona = seller_persona_map[uid]
                        events, counts = self._build_seller_events(
                            uid=uid,
                            session_id=session_id,
                            persona=persona,
                            product_id=product_id,
                            category=category,
                            brand=brand,
                            size=size,
                            condition=condition,
                            price=base_price,
                            device=device,
                            referrer=referrer,
                            occurred_at=ts,
                            experiment_id=req.experiment_id,
                            variant_id=variant_id,
                        )
                    events_batch.extend(events)
                    for k, v in counts.items():
                        by_event[k] += v

            total_events += self.event_repo.bulk_insert(events_batch)

        ended_at = datetime.now(timezone.utc)
        return SimulatorRunResponse(
            users=req.users,
            sessions=total_sessions,
            events_inserted=total_events,
            by_event=by_event,
            started_at=started_at,
            ended_at=ended_at,
        )

    def _assign_variants(
        self,
        user_ids: List[uuid.UUID],
        experiment_id: Optional[str],
        splits: Optional[Dict[str, float]],
    ) -> Dict[uuid.UUID, Optional[str]]:
        if not experiment_id:
            return {uid: None for uid in user_ids}

        if not splits:
            splits = {"A": 0.5, "B": 0.5}

        total = sum(splits.values())
        if total <= 0:
            splits = {"A": 0.5, "B": 0.5}
            total = 1.0

        normalized: List[Tuple[str, float]] = []
        cumulative = 0.0
        for key, val in splits.items():
            cumulative += val / total
            normalized.append((key, cumulative))

        variant_map: Dict[uuid.UUID, str] = {}
        for uid in user_ids:
            h = hashlib.md5(str(uid).encode("utf-8")).hexdigest()
            r = int(h[:8], 16) / 0xFFFFFFFF
            for key, threshold in normalized:
                if r <= threshold:
                    variant_map[uid] = key
                    break
        return variant_map

    def _build_buyer_events(
        self,
        uid: uuid.UUID,
        session_id: str,
        persona: BuyerProfile,
        product_id: str,
        category: str,
        brand: str,
        size: str,
        condition: str,
        price: int,
        device: str,
        referrer: str,
        occurred_at: datetime,
        experiment_id: Optional[str],
        variant_id: Optional[str],
    ) -> Tuple[List[AnalyticsEvent], Dict[str, int]]:
        events: List[AnalyticsEvent] = []
        counts = {name: 0 for name in EVENT_NAMES}

        def emit(event_name: str, offset_sec: int, props: Dict) -> None:
            events.append(
                AnalyticsEvent(
                    event_name=event_name,
                    user_id=uid,
                    session_id=session_id,
                    source_service="simulator",
                    occurred_at=occurred_at + timedelta(seconds=offset_sec),
                    properties=props,
                    experiment_id=experiment_id,
                    variant_id=variant_id,
                )
            )
            counts[event_name] += 1

        base_props = {
            "product_id": product_id,
            "category": category,
            "brand": brand,
            "size": size,
            "condition": condition,
            "price": price,
            "device": device,
            "referrer": referrer,
            "persona": persona.name,
            "trade_side": "buy",
        }

        emit("view_item", 0, base_props)

        if random.random() <= persona.wishlist_p:
            emit("add_to_wishlist", 5, base_props)

        def emit_checkout_flow(offset_sec: int, paid_price: int, props: Dict) -> None:
            emit("checkout_start", offset_sec, props)
            if random.random() <= persona.payment_success_p:
                emit("payment_completed", offset_sec + 20, {**props, "paid_price": paid_price})
            else:
                emit(
                    "payment_failed",
                    offset_sec + 20,
                    {**props, "failure_reason": random.choice(PAYMENT_FAIL_REASONS)},
                )

        if random.random() <= persona.buy_now_p:
            attempt_props = {**base_props, "flow": "buy_now"}
            emit("buy_now_attempt", 8, attempt_props)

            if random.random() <= LOCK_FAIL_P:
                emit("lock_failed", 9, attempt_props)
                if variant_id == "B":
                    next_best_price = int(price * random.uniform(*NEXT_BEST_PRICE_MULTIPLIER))
                    price_delta = next_best_price - price
                    offer_props = {
                        **attempt_props,
                        "next_best_price": next_best_price,
                        "price_delta": price_delta,
                    }
                    emit("next_best_offered", 10, offer_props)
                    if random.random() <= persona.next_best_accept_p:
                        emit("next_best_accepted", 12, offer_props)
                        emit_checkout_flow(15, next_best_price, offer_props)
                    else:
                        emit(
                            "checkout_abandoned",
                            15,
                            {**offer_props, "abandon_reason": random.choice(NEXT_BEST_DECLINE_REASONS)},
                        )
                return events, counts

            emit_checkout_flow(10, price, attempt_props)
            return events, counts

        if random.random() <= persona.bid_p:
            bid_id = str(uuid.uuid4())
            bid_price = int(price * random.uniform(0.85, 1.05))
            ask_price = int(price * random.uniform(0.9, 1.1))
            spread = ask_price - bid_price
            emit(
                "place_bid",
                10,
                {
                    **base_props,
                    "bid_id": bid_id,
                    "bid_price": bid_price,
                    "ask_price": ask_price,
                    "spread": spread,
                },
            )

            match_prob = self._match_probability(ask_price, bid_price, price)
            if random.random() <= match_prob:
                match_latency = random.randint(60, 3600)
                match_id = str(uuid.uuid4())
                emit(
                    "match_bid",
                    10 + match_latency,
                    {
                        **base_props,
                        "bid_id": bid_id,
                        "match_id": match_id,
                        "bid_price": bid_price,
                        "ask_price": ask_price,
                        "spread": spread,
                        "match_latency_sec": match_latency,
                    },
                )
                checkout_prob = persona.match_to_checkout_p
                if variant_id == "B":
                    checkout_prob += 0.10
                if match_latency >= 1800:
                    checkout_prob -= 0.05
                checkout_prob = max(0.05, min(0.95, checkout_prob))

                checkout_props = {
                    **base_props,
                    "flow": "bid",
                    "bid_id": bid_id,
                    "match_id": match_id,
                    "bid_price": bid_price,
                    "ask_price": ask_price,
                    "spread": spread,
                }

                if random.random() <= checkout_prob:
                    emit_checkout_flow(20 + match_latency, bid_price, checkout_props)
                else:
                    emit(
                        "checkout_abandoned",
                        20 + match_latency,
                        {
                            **checkout_props,
                            "abandon_reason": random.choice(CHECKOUT_ABANDON_REASONS),
                        },
                    )
            else:
                if random.random() <= 0.35:
                    emit("bid_expired", 3600, base_props)

        return events, counts

    def _build_seller_events(
        self,
        uid: uuid.UUID,
        session_id: str,
        persona: SellerProfile,
        product_id: str,
        category: str,
        brand: str,
        size: str,
        condition: str,
        price: int,
        device: str,
        referrer: str,
        occurred_at: datetime,
        experiment_id: Optional[str],
        variant_id: Optional[str],
    ) -> Tuple[List[AnalyticsEvent], Dict[str, int]]:
        events: List[AnalyticsEvent] = []
        counts = {name: 0 for name in EVENT_NAMES}

        def emit(event_name: str, offset_sec: int, props: Dict) -> None:
            events.append(
                AnalyticsEvent(
                    event_name=event_name,
                    user_id=uid,
                    session_id=session_id,
                    source_service="simulator",
                    occurred_at=occurred_at + timedelta(seconds=offset_sec),
                    properties=props,
                    experiment_id=experiment_id,
                    variant_id=variant_id,
                )
            )
            counts[event_name] += 1

        inventory_age = random.randint(0, 45)
        listing_id = str(uuid.uuid4())
        ask_price = int(price * random.uniform(0.9, 1.1))
        expected_bid = int(price * random.uniform(0.85, 1.05))
        spread = ask_price - expected_bid

        base_props = {
            "product_id": product_id,
            "category": category,
            "brand": brand,
            "size": size,
            "condition": condition,
            "price": price,
            "device": device,
            "referrer": referrer,
            "persona": persona.name,
            "trade_side": "sell",
            "listing_id": listing_id,
            "inventory_age_days": inventory_age,
        }

        emit("list_item", 0, base_props)
        emit("set_ask", 5, {**base_props, "ask_price": ask_price})

        match_prob = min(1.0, persona.match_p + self._match_probability(ask_price, expected_bid, price))
        if random.random() <= match_prob:
            match_latency = random.randint(300, 7200)
            match_id = str(uuid.uuid4())
            emit(
                "match_bid",
                5 + match_latency,
                {
                    **base_props,
                    "match_id": match_id,
                    "ask_price": ask_price,
                    "bid_price": expected_bid,
                    "spread": spread,
                    "match_latency_sec": match_latency,
                },
            )
            if random.random() <= persona.complete_p:
                emit("sell_completed", 5 + match_latency + 7200, {**base_props, "sell_price": ask_price})

        return events, counts

    @staticmethod
    def _match_probability(ask_price: int, bid_price: int, base_price: int) -> float:
        if base_price <= 0:
            return 0.1
        if bid_price >= ask_price:
            return 0.8
        spread = max(ask_price - bid_price, 0)
        spread_ratio = spread / base_price
        return max(0.05, 0.45 - (spread_ratio * 1.5))

    @staticmethod
    def _random_time_on_day(day: date) -> datetime:
        start = datetime.combine(day, time.min, tzinfo=timezone.utc)
        offset = random.randint(0, 86399)
        return start + timedelta(seconds=offset)
