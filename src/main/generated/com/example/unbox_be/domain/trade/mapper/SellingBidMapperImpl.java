package com.example.unbox_be.domain.trade.mapper;

import com.example.unbox_be.domain.trade.dto.request.SellingBidRequestDto;
import com.example.unbox_be.domain.trade.dto.response.SellingBidResponseDto;
import com.example.unbox_be.domain.trade.entity.SellingBid;
import java.time.LocalDateTime;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-01-02T07:50:06+0900",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 17.0.12 (Oracle Corporation)"
)
@Component
public class SellingBidMapperImpl implements SellingBidMapper {

    @Override
    public SellingBid toEntity(SellingBidRequestDto dto, LocalDateTime deadline) {
        if ( dto == null && deadline == null ) {
            return null;
        }

        SellingBid.SellingBidBuilder sellingBid = SellingBid.builder();

        if ( dto != null ) {
            sellingBid.userId( dto.getUserId() );
            sellingBid.optionId( dto.getOptionId() );
            sellingBid.price( dto.getPrice() );
        }
        sellingBid.deadline( deadline );

        return sellingBid.build();
    }

    @Override
    public SellingBidResponseDto toResponseDto(SellingBid entity) {
        if ( entity == null ) {
            return null;
        }

        SellingBidResponseDto.SellingBidResponseDtoBuilder sellingBidResponseDto = SellingBidResponseDto.builder();

        sellingBidResponseDto.sellingId( entity.getSellingId() );
        sellingBidResponseDto.status( entity.getStatus() );
        sellingBidResponseDto.price( entity.getPrice() );
        sellingBidResponseDto.deadline( entity.getDeadline() );

        return sellingBidResponseDto.build();
    }
}
