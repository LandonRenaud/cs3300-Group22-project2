package com.cs3300.project2.meal;

/**
 * How one meal ingredient compares with the pantry.
 *
 * @param needed  amount the meal calls for
 * @param onHand  unexpired pantry amount remaining before this line, ignoring case and unit;
 *                stock used by an earlier line in the same meal is excluded
 * @param toBuy   amount still missing; zero when the pantry already has enough
 */
public record ShoppingListItem(String name, String unit, double needed, double onHand, double toBuy) {
}
