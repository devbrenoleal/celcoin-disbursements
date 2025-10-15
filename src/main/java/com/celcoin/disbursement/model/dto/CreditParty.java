package com.celcoin.disbursement.model.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Getter @Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@EqualsAndHashCode
public class CreditParty implements Serializable {
    private String Key;
    private String account;
    private String accountType;
    private String bank;
    private String branch;
    private String name;
    private String taxId;
}
