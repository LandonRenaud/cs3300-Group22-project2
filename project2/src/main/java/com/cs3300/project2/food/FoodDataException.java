package com.cs3300.project2.food;

public class FoodDataException extends RuntimeException {
    public FoodDataException(String message) { super(message); }
    public FoodDataException(String message, Throwable cause) { super(message, cause); }
}
