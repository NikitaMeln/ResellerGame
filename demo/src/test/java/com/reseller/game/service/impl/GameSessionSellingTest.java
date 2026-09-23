package com.reseller.game.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.reseller.game.exception.IllegalActionException;
import com.reseller.game.model.entity.Client;
import com.reseller.game.model.entity.Tuning;
import com.reseller.game.session.GameSession;

/**
 * Selling phase: what a sale pays out, what a failed sale leaves behind, and when the game is won.
 *
 * All sessions here are single-player so the turn order stays trivial. The dice threshold equals
 * the client's randomCounter because every tuning and secret card in the fixture has no
 * addToRandomModifier, so a roll of 6 against randomCounter 3 always wins and a roll of 1 against
 * randomCounter 6 always loses.
 */
@ExtendWith(MockitoExtension.class)
class GameSessionSellingTest extends GameSessionTestSupport {

    private static final int CAR_PRICE = 500;
    private static final int BUDGET = 2000;

    private void sessionWithWinnableSale() {
        startSession(identicalCars(2, CAR_PRICE), identicalClients(2, BUDGET, 3, true), neutralNegativeCards(3), P1);
    }

    private void sessionWithLosingSale() {
        startSession(identicalCars(2, CAR_PRICE), identicalClients(2, BUDGET, 6, true), neutralNegativeCards(3), P1);
    }

    @Test
    void aSuccessfulSaleAddsTheClientBudgetToTheBalance() {
        sessionWithWinnableSale();
        playUntilDiceRoll(P1);

        service.processRollDiceAction(ROOM, P1, 6);

        assertThat(balanceOf(P1)).isEqualTo(STARTING_BALANCE - CAR_PRICE + BUDGET);
    }

    @Test
    void aSuccessfulSaleIncrementsSoldCars() {
        sessionWithWinnableSale();
        playUntilDiceRoll(P1);

        service.processRollDiceAction(ROOM, P1, 6);

        assertThat(service.getSession(ROOM).findPlayer(P1).getSoldCars()).isEqualTo(1);
    }

    @Test
    void aSuccessfulSaleAddsTheClientBudgetToTotalProfit() {
        sessionWithWinnableSale();
        playUntilDiceRoll(P1);

        service.processRollDiceAction(ROOM, P1, 6);

        assertThat(service.getSession(ROOM).findPlayer(P1).getTotalProfit()).isEqualTo(BUDGET);
    }

    @Test
    void aSuccessfulSaleRemovesTheCarFromTheGarage() {
        sessionWithWinnableSale();
        playUntilDiceRoll(P1);

        service.processRollDiceAction(ROOM, P1, 6);

        assertThat(service.getSession(ROOM).findPlayer(P1).getGarage()).isEmpty();
    }

    @Test
    void aSuccessfulSaleIsRecordedOnTheCurrentSale() {
        sessionWithWinnableSale();
        playUntilDiceRoll(P1);

        service.processRollDiceAction(ROOM, P1, 6);

        assertThat(service.getSession(ROOM).getCurrentSale().getSuccess()).isTrue();
    }

    @Test
    void theClientLeavesTheWindowAfterASuccessfulSale() {
        sessionWithWinnableSale();
        playUntilDiceRoll(P1);
        long clientId = service.getSession(ROOM).getCurrentSale().getClientId();
        service.processRollDiceAction(ROOM, P1, 6);

        service.processNextTurnAction(ROOM, P1);

        assertThat(service.getSession(ROOM).getVisibleClients())
                .extracting(Client::getId)
                .doesNotContain(clientId);
    }

    @Test
    void aFailedSaleKeepsTheCarInThePlayersGarage() {
        sessionWithLosingSale();
        playUntilDiceRoll(P1);
        String carInstanceId = garageCarId(P1);

        service.processRollDiceAction(ROOM, P1, 1);

        assertThat(service.getSession(ROOM).findPlayer(P1).getGarage())
                .extracting(car -> car.getInstanceId())
                .containsExactly(carInstanceId);
    }

    @Test
    void aFailedSaleLeavesTheBalanceUnchanged() {
        sessionWithLosingSale();
        playUntilDiceRoll(P1);

        service.processRollDiceAction(ROOM, P1, 1);

        assertThat(balanceOf(P1)).isEqualTo(STARTING_BALANCE - CAR_PRICE);
    }

    @Test
    void aFailedSaleDoesNotIncrementSoldCars() {
        sessionWithLosingSale();
        playUntilDiceRoll(P1);

        service.processRollDiceAction(ROOM, P1, 1);

        assertThat(service.getSession(ROOM).findPlayer(P1).getSoldCars()).isZero();
    }

    @Test
    void aFailedSaleDoesNotChangeTotalProfit() {
        sessionWithLosingSale();
        playUntilDiceRoll(P1);

        service.processRollDiceAction(ROOM, P1, 1);

        assertThat(service.getSession(ROOM).findPlayer(P1).getTotalProfit()).isZero();
    }

    @Test
    void aFailedSalePaysNothing() {
        sessionWithLosingSale();
        playUntilDiceRoll(P1);

        service.processRollDiceAction(ROOM, P1, 1);

        assertThat(service.getSession(ROOM).getCurrentSale().getProfit()).isZero();
    }

    @Test
    void theClientLeavesTheWindowEvenAfterAFailedSale() {
        sessionWithLosingSale();
        playUntilDiceRoll(P1);
        long clientId = service.getSession(ROOM).getCurrentSale().getClientId();
        service.processRollDiceAction(ROOM, P1, 1);

        service.processNextTurnAction(ROOM, P1);

        assertThat(service.getSession(ROOM).getVisibleClients())
                .extracting(Client::getId)
                .doesNotContain(clientId);
    }

    @Test
    void theCarAndTuningAreChargedAtPurchaseAndNotAgainOnSale() {
        int tuningPrice = 200;
        startSession(identicalCars(2, CAR_PRICE), identicalClients(2, BUDGET, 3, false),
                tuningPool(3, positiveTuning(1L, tuningPrice), positiveTuning(2L, tuningPrice)), P1);
        service.processBuyCarAction(ROOM, P1, firstVisibleCarId());
        service.processBuyTuningAction(ROOM, P1, firstVisibleTuningId(), garageCarId(P1));
        service.processChooseClientAndCarAction(ROOM, P1, firstVisibleClientId(), garageCarId(P1));
        service.processRevealSecretCardAction(ROOM, P1);

        service.processRollDiceAction(ROOM, P1, 6);

        assertThat(balanceOf(P1)).isEqualTo(STARTING_BALANCE - CAR_PRICE - tuningPrice + BUDGET);
    }

    @Test
    void theSalePaysTheFullClientBudgetWhateverTheCarCost() {
        int expensiveCar = 1200;
        startSession(identicalCars(2, expensiveCar), identicalClients(2, BUDGET, 3, true),
                neutralNegativeCards(3), P1);
        playUntilDiceRoll(P1);

        service.processRollDiceAction(ROOM, P1, 6);

        assertThat(service.getSession(ROOM).findPlayer(P1).getTotalProfit()).isEqualTo(BUDGET);
    }

    @Test
    void theGameIsWonAfterFiveSuccessfulSales() {
        // budget 1500 keeps total profit at 7500 after five sales, so the win can only come from the sold-car count
        startSession(identicalCars(12, CAR_PRICE), identicalClients(12, 1500, 3, true), neutralNegativeCards(12), P1);

        for (int round = 0; round < 5; round++) {
            playWinningRound(P1);
        }

        assertThat(service.getSession(ROOM).getWinnerTelegramId()).isEqualTo(P1);
    }

    @Test
    void theGameIsNotWonAfterOnlyFourSuccessfulSales() {
        startSession(identicalCars(12, CAR_PRICE), identicalClients(12, 1500, 3, true), neutralNegativeCards(12), P1);

        for (int round = 0; round < 4; round++) {
            playWinningRound(P1);
        }

        assertThat(service.getSession(ROOM).getWinnerTelegramId()).isNull();
    }

    @Test
    void theGameIsWonOnTotalProfitBeforeFiveCarsAreSold() {
        // three sales at 3500 reach 10500, above the 10000 profit threshold, with only three cars sold
        startSession(identicalCars(12, CAR_PRICE), identicalClients(12, 3500, 3, true), neutralNegativeCards(12), P1);

        for (int round = 0; round < 3; round++) {
            playWinningRound(P1);
        }

        GameSession session = service.getSession(ROOM);
        assertThat(session.getWinnerTelegramId()).isEqualTo(P1);
        assertThat(session.findPlayer(P1).getSoldCars()).isEqualTo(3);
    }

    @Test
    void theGameIsNotWonWhileTotalProfitStaysBelowTheThreshold() {
        startSession(identicalCars(12, CAR_PRICE), identicalClients(12, 3500, 3, true), neutralNegativeCards(12), P1);

        for (int round = 0; round < 2; round++) {
            playWinningRound(P1);
        }

        assertThat(service.getSession(ROOM).getWinnerTelegramId()).isNull();
    }

    @Test
    void aPositiveTuningLowersTheDiceThreshold() {
        // client alone would need 5; the tuning's -2 brings the threshold down to 3, so a 3 now wins
        List<Tuning> tunings = new ArrayList<>(neutralNegativeCards(3));
        tunings.add(positiveTuningWithModifier(1L, 200, -2));
        tunings.add(positiveTuningWithModifier(2L, 200, -2));
        startSession(identicalCars(2, CAR_PRICE), identicalClients(2, BUDGET, 5, false), tunings, P1);
        service.processBuyCarAction(ROOM, P1, firstVisibleCarId());
        service.processBuyTuningAction(ROOM, P1, firstVisibleTuningId(), garageCarId(P1));
        service.processChooseClientAndCarAction(ROOM, P1, firstVisibleClientId(), garageCarId(P1));
        service.processRevealSecretCardAction(ROOM, P1);

        service.processRollDiceAction(ROOM, P1, 3);

        assertThat(service.getSession(ROOM).getCurrentSale().getSuccess()).isTrue();
    }

    @Test
    void aRevealedSecretCardRaisesTheDiceThreshold() {
        // client alone would need 3; the card's +2 pushes the threshold to 5, so a 4 now loses
        startSession(identicalCars(2, CAR_PRICE), identicalClients(2, BUDGET, 3, true),
                negativeCardsWithModifier(3, 2), P1);
        playUntilDiceRoll(P1);

        service.processRollDiceAction(ROOM, P1, 4);

        assertThat(service.getSession(ROOM).getCurrentSale().getSuccess()).isFalse();
    }

    @Test
    void theThresholdUsedForTheRollIsRecordedOnTheSale() {
        startSession(identicalCars(2, CAR_PRICE), identicalClients(2, BUDGET, 3, true),
                negativeCardsWithModifier(3, 2), P1);
        playUntilDiceRoll(P1);

        service.processRollDiceAction(ROOM, P1, 4);

        assertThat(service.getSession(ROOM).getCurrentSale().getThreshold()).isEqualTo(5);
    }

    @Test
    void skippingInTheMiddleOfASaleDropsTheHalfFinishedSale() {
        // the session's currentSale is broadcast to every client, so an abandoned sale must not linger
        sessionWithWinnableSale();
        playUntilDiceRoll(P1);

        service.processSkipAction(ROOM, P1);

        assertThat(service.getSession(ROOM).getCurrentSale()).isNull();
    }

    @Test
    void aClientShoppingForADifferentDecadeIsRejected() {
        List<Client> eighties = List.of(client(1L, BUDGET, 3, true, "2010-x"), client(2L, BUDGET, 3, true, "2010-x"));
        startSession(identicalCars(2, CAR_PRICE), eighties, neutralNegativeCards(3), P1);
        service.processBuyCarAction(ROOM, P1, firstVisibleCarId());
        service.processSkipAction(ROOM, P1);
        long clientId = firstVisibleClientId();
        String carId = garageCarId(P1);

        assertThatThrownBy(() -> service.processChooseClientAndCarAction(ROOM, P1, clientId, carId))
                .isInstanceOf(IllegalActionException.class);
    }

    @Test
    void aClientWhoWantsAStockCarRejectsATunedOne() {
        startSession(identicalCars(2, CAR_PRICE), identicalClients(2, BUDGET, 3, true),
                tuningPool(3, positiveTuning(1L, 200), positiveTuning(2L, 200)), P1);
        service.processBuyCarAction(ROOM, P1, firstVisibleCarId());
        service.processBuyTuningAction(ROOM, P1, firstVisibleTuningId(), garageCarId(P1));
        long clientId = firstVisibleClientId();
        String carId = garageCarId(P1);

        assertThatThrownBy(() -> service.processChooseClientAndCarAction(ROOM, P1, clientId, carId))
                .isInstanceOf(IllegalActionException.class);
    }

    @Test
    void aDiceValueOutsideOneToSixIsRejected() {
        sessionWithWinnableSale();
        playUntilDiceRoll(P1);

        assertThatThrownBy(() -> service.processRollDiceAction(ROOM, P1, 7))
                .isInstanceOf(IllegalActionException.class);
    }

    @Test
    void rollingTheDiceBeforeTheSecretCardIsRevealedIsRejected() {
        sessionWithWinnableSale();
        service.processBuyCarAction(ROOM, P1, firstVisibleCarId());
        service.processSkipAction(ROOM, P1);
        service.processChooseClientAndCarAction(ROOM, P1, firstVisibleClientId(), garageCarId(P1));

        assertThatThrownBy(() -> service.processRollDiceAction(ROOM, P1, 6))
                .isInstanceOf(IllegalActionException.class);
    }
}
