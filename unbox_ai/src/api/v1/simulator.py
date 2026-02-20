from fastapi import APIRouter, Depends, HTTPException
from src.api.dependencies import get_simulator_service
from src.dto.common_dto import BaseResponse
from src.dto.simulator_dto import SimulatorRunRequest, SimulatorRunResponse
from src.service.simulator_service import SimulatorService

router = APIRouter(prefix="/simulator", tags=["Simulator"])


@router.post("/run", response_model=BaseResponse[SimulatorRunResponse])
def run_simulator(
    payload: SimulatorRunRequest,
    service: SimulatorService = Depends(get_simulator_service),
):
    try:
        result = service.run(payload)
        return BaseResponse(success=True, message="Simulation completed", data=result)
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
