import logging
from apscheduler.schedulers.asyncio import AsyncIOScheduler
from apscheduler.triggers.cron import CronTrigger
from src.scheduler.jobs import refresh_target_shoes_price

logger = logging.getLogger(__name__)

# TODO: 추후 DB나 설정 파일에서 관리하도록 변경 필요
TARGET_SHOES_LIST = [
    "Jordan 1 Chicago",
    "Nike Dunk Low Black",
    "Yeezy Boost 350",
]

class SchedulerRunner:
    """
    APScheduler를 관리하는 클래스 (Singleton 패턴처럼 사용)
    """
    def __init__(self):
        # 비동기 함수(coroutine)를 실행해야 하므로 AsyncIOScheduler 사용
        self.scheduler = AsyncIOScheduler()
        
    def start(self):
        """스케줄러 시작 및 Job 등록"""
        if not self.scheduler.running:
            # 1. Job 추가 (매일 새벽 4시 실행)
            # jitter: 60초 (분산 실행)
            self.scheduler.add_job(
                refresh_target_shoes_price,
                trigger=CronTrigger(hour=4, minute=0),
                args=[TARGET_SHOES_LIST],
                id="job_refresh_market_prices",
                replace_existing=True,
                jitter=60
            )
            
            # 2. 스케줄러 시작
            self.scheduler.start()
            logger.info("Scheduler Started: 시세 업데이트 작업이 매일 새벽 4시에 실행됩니다.")
            
    def shutdown(self):
        """스케줄러 종료"""
        if self.scheduler.running:
            self.scheduler.shutdown()
            logger.info("Scheduler Shutdown")

# 전역 인스턴스 생성 (main.py에서 import하여 사용)
scheduler_runner = SchedulerRunner()
