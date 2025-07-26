package com.snappfood.model;

import com.google.gson.annotations.SerializedName;

public class BankInfo {

    @SerializedName("bank_name")
    private String bankName;

    @SerializedName("account_number")
    private String accountNumber;


    public BankInfo() {
    }

    public BankInfo(String bankName, String accountNumber) {
        this.bankName = bankName;
        this.accountNumber = accountNumber;
    }

    public String getBankName() {
        return bankName;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }
}