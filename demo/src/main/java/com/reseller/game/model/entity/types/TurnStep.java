package com.reseller.game.model.entity.types;

public enum TurnStep {
    CAR_SELECTION, // Buying a car
    TUNING_SELECTION, // Buying additional tuning for your car
// Wait until all players have completed their turn
    CHOICE_CLIENT_TO_SELL, // Selecting a suitable client to sell to
    SHOW_SECRET_CARD, // Showing the player the NEGATIVE card
    TURN_MULTIPLIER, // Rolling the dice and, if the random number is correct, successfully selling or not selling, then the turn passes to the next player.
    RESULT // Calculating all bonuses and debuffs on the car in relation to the transaction amount
// Now updating all zones except the garage zone and starting a new round.
}
