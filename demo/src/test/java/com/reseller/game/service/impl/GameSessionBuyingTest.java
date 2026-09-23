package com.reseller.game.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.reseller.game.exception.CarNotFoundException;
import com.reseller.game.exception.GarageFullException;
import com.reseller.game.exception.InsufficientBalanceException;
import com.reseller.game.exception.PlayerNotFoundException;
import com.reseller.game.model.entity.types.TurnStep;
import com.reseller.game.session.CarInstance;
import com.reseller.game.session.GameSession;

@ExtendWith(MockitoExtension.class)
class GameSessionBuyingTest extends GameSessionTestSupport {

    @Test
    void everyPlayerStartsWithTheStartingBalance() {
        GameSession session = startSession(identicalCars(2, 500), identicalClients(2, 2000, 3, true),
                neutralNegativeCards(3), P1);

        assertThat(session.findPlayer(P1).getBalance()).isEqualTo(STARTING_BALANCE);
    }

    @Test
    void buyingACarDeductsItsPriceFromTheBalance() {
        startSession(identicalCars(2, 500), identicalClients(2, 2000, 3, true), neutralNegativeCards(3), P1);

        service.buyCar(ROOM, P1, firstVisibleCarId());

        assertThat(balanceOf(P1)).isEqualTo(STARTING_BALANCE - 500);
    }

    @Test
    void buyingACarPutsItInThePlayersGarage() {
        startSession(identicalCars(2, 500), identicalClients(2, 2000, 3, true), neutralNegativeCards(3), P1);
        long carId = firstVisibleCarId();

        CarInstance bought = service.buyCar(ROOM, P1, carId);

        assertThat(service.getSession(ROOM).findPlayer(P1).getGarage()).containsExactly(bought);
    }

    @Test
    void buyingACarRemovesItFromTheShopWindow() {
        startSession(identicalCars(2, 500), identicalClients(2, 2000, 3, true), neutralNegativeCards(3), P1);
        long carId = firstVisibleCarId();

        service.buyCar(ROOM, P1, carId);

        assertThat(service.getSession(ROOM).getVisibleCars())
                .extracting(car -> car.getId())
                .doesNotContain(carId);
    }

    @Test
    void buyingACarThePlayerCannotAffordIsRejected() {
        startSession(identicalCars(2, 2000), identicalClients(2, 2000, 3, true), neutralNegativeCards(3), P1);
        long carId = firstVisibleCarId();

        assertThatThrownBy(() -> service.buyCar(ROOM, P1, carId))
                .isInstanceOf(InsufficientBalanceException.class);
    }

    @Test
    void aRejectedPurchaseLeavesTheBalanceUntouched() {
        startSession(identicalCars(2, 2000), identicalClients(2, 2000, 3, true), neutralNegativeCards(3), P1);
        long carId = firstVisibleCarId();

        assertThatThrownBy(() -> service.buyCar(ROOM, P1, carId))
                .isInstanceOf(InsufficientBalanceException.class);

        assertThat(balanceOf(P1)).isEqualTo(STARTING_BALANCE);
    }

    @Test
    void aRejectedPurchaseLeavesTheGarageEmpty() {
        startSession(identicalCars(2, 2000), identicalClients(2, 2000, 3, true), neutralNegativeCards(3), P1);
        long carId = firstVisibleCarId();

        assertThatThrownBy(() -> service.buyCar(ROOM, P1, carId))
                .isInstanceOf(InsufficientBalanceException.class);

        assertThat(service.getSession(ROOM).findPlayer(P1).getGarage()).isEmpty();
    }

    @Test
    void buyingAFourthCarIsRejectedBecauseTheGarageHoldsThree() {
        // three players -> a four-card window, so one player can reach the garage limit
        startSession(identicalCars(4, 100), identicalClients(4, 2000, 3, true), neutralNegativeCards(5), P1, P2, P3);
        List<Long> carIds = service.getSession(ROOM).getVisibleCars().stream().map(car -> car.getId()).toList();
        service.buyCar(ROOM, P1, carIds.get(0));
        service.buyCar(ROOM, P1, carIds.get(1));
        service.buyCar(ROOM, P1, carIds.get(2));

        assertThatThrownBy(() -> service.buyCar(ROOM, P1, carIds.get(3)))
                .isInstanceOf(GarageFullException.class);
    }

    @Test
    void aPurchaseRejectedByAFullGarageDoesNotChargeThePlayer() {
        startSession(identicalCars(4, 100), identicalClients(4, 2000, 3, true), neutralNegativeCards(5), P1, P2, P3);
        List<Long> carIds = service.getSession(ROOM).getVisibleCars().stream().map(car -> car.getId()).toList();
        service.buyCar(ROOM, P1, carIds.get(0));
        service.buyCar(ROOM, P1, carIds.get(1));
        service.buyCar(ROOM, P1, carIds.get(2));

        assertThatThrownBy(() -> service.buyCar(ROOM, P1, carIds.get(3)))
                .isInstanceOf(GarageFullException.class);

        assertThat(balanceOf(P1)).isEqualTo(STARTING_BALANCE - 300);
    }

    @Test
    void buyingACarThatIsNotInTheShopWindowIsRejected() {
        startSession(identicalCars(2, 500), identicalClients(2, 2000, 3, true), neutralNegativeCards(3), P1);

        assertThatThrownBy(() -> service.buyCar(ROOM, P1, 999L))
                .isInstanceOf(CarNotFoundException.class);
    }

    @Test
    void buyingForAnUnknownPlayerIsRejected() {
        startSession(identicalCars(2, 500), identicalClients(2, 2000, 3, true), neutralNegativeCards(3), P1);
        long carId = firstVisibleCarId();

        assertThatThrownBy(() -> service.buyCar(ROOM, "ghost", carId))
                .isInstanceOf(PlayerNotFoundException.class);
    }

    @Test
    void buyingATuningDeductsItsPrice() {
        startSession(identicalCars(2, 500), identicalClients(2, 2000, 3, true),
                tuningPool(3, positiveTuning(1L, 200), positiveTuning(2L, 200)), P1);
        service.buyCar(ROOM, P1, firstVisibleCarId());

        service.buyTuning(ROOM, P1, firstVisibleTuningId(), garageCarId(P1));

        assertThat(balanceOf(P1)).isEqualTo(STARTING_BALANCE - 500 - 200);
    }

    @Test
    void buyingATuningAppliesItToTheChosenCar() {
        startSession(identicalCars(2, 500), identicalClients(2, 2000, 3, true),
                tuningPool(3, positiveTuning(1L, 200), positiveTuning(2L, 200)), P1);
        service.buyCar(ROOM, P1, firstVisibleCarId());
        long tuningId = firstVisibleTuningId();

        service.buyTuning(ROOM, P1, tuningId, garageCarId(P1));

        assertThat(service.getSession(ROOM).findPlayer(P1).getGarage().get(0).getAppliedTunings())
                .extracting(tuning -> tuning.getId())
                .containsExactly(tuningId);
    }

    @Test
    void buyingATuningRemovesItFromTheShopWindow() {
        startSession(identicalCars(2, 500), identicalClients(2, 2000, 3, true),
                tuningPool(3, positiveTuning(1L, 200), positiveTuning(2L, 200)), P1);
        service.buyCar(ROOM, P1, firstVisibleCarId());
        long tuningId = firstVisibleTuningId();

        service.buyTuning(ROOM, P1, tuningId, garageCarId(P1));

        assertThat(service.getSession(ROOM).getVisibleTunings())
                .extracting(tuning -> tuning.getId())
                .doesNotContain(tuningId);
    }

    @Test
    void buyingATuningThePlayerCannotAffordIsRejected() {
        startSession(identicalCars(2, 1200), identicalClients(2, 2000, 3, true),
                tuningPool(3, positiveTuning(1L, 200), positiveTuning(2L, 200)), P1);
        service.buyCar(ROOM, P1, firstVisibleCarId());
        long tuningId = firstVisibleTuningId();
        String carId = garageCarId(P1);

        assertThatThrownBy(() -> service.buyTuning(ROOM, P1, tuningId, carId))
                .isInstanceOf(InsufficientBalanceException.class);
    }

    @Test
    void aRejectedTuningIsNotAppliedToTheCar() {
        startSession(identicalCars(2, 1200), identicalClients(2, 2000, 3, true),
                tuningPool(3, positiveTuning(1L, 200), positiveTuning(2L, 200)), P1);
        service.buyCar(ROOM, P1, firstVisibleCarId());
        long tuningId = firstVisibleTuningId();
        String carId = garageCarId(P1);

        assertThatThrownBy(() -> service.buyTuning(ROOM, P1, tuningId, carId))
                .isInstanceOf(InsufficientBalanceException.class);

        assertThat(service.getSession(ROOM).findPlayer(P1).getGarage().get(0).getAppliedTunings()).isEmpty();
    }

    @Test
    void buyingACarMovesTheTurnOnToTuningSelection() {
        startSession(identicalCars(2, 500), identicalClients(2, 2000, 3, true), neutralNegativeCards(3), P1);

        service.processBuyCarAction(ROOM, P1, firstVisibleCarId());

        assertThat(service.getSession(ROOM).getTurnStep()).isEqualTo(TurnStep.TUNING_SELECTION);
    }

    @Test
    void buyingACarAttachesAFaceDownSecretCard() {
        startSession(identicalCars(2, 500), identicalClients(2, 2000, 3, true), neutralNegativeCards(3), P1);

        service.processBuyCarAction(ROOM, P1, firstVisibleCarId());

        CarInstance bought = service.getSession(ROOM).findPlayer(P1).getGarage().get(0);
        assertThat(bought.getHiddenNegativeCard()).isNotNull();
        assertThat(bought.isNegativeCardRevealed()).isFalse();
    }
}
