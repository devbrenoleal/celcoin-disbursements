package com.celcoin.disbursement.model.dto;

import com.celcoin.disbursement.model.utils.Recurrency;
import com.celcoin.disbursement.model.utils.ScheduleType;

import java.time.LocalDateTime;

public record Schedule(ScheduleType type, LocalDateTime date, Recurrency recurrency) {
}
