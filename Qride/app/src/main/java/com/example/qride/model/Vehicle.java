package com.example.qride.model;

public class Vehicle {
    public String plate;
    public int pin;
    public String status;

    public Vehicle(String plate, int pin, String status) {
        this.plate = plate;
        this.pin = pin;
        this.status = status;
    }

    public String getPlate() {
        return plate;
    }

    public void setPlate(String plate) {
        this.plate = plate;
    }

    public int getPin() {
        return pin;
    }

    public void setPin(int pin) {
        this.pin = pin;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
