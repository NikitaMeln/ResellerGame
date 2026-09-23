package com.reseller.game.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.reseller.game.exception.IllegalActionException;
import com.reseller.game.model.entity.Car;
import com.reseller.game.model.entity.types.RoomPhase;
import com.reseller.game.model.entity.types.TurnStep;
import com.reseller.game.session.GameSession;

/**
 * Turn order, phase switching and session lifecycle.
 *
 * A round is one pass through BUYING in queue order followed by one pass through SELLING in
 * reverse order; the shop window is refreshed when SELLING wraps back around to BUYING.
 */
@ExtendWith(MockitoExtension.class)
class GameSessionTurnOrderTest extends GameSessionTestSupport {

    private void threePlayerSession() {
        startSession(identicalCars(4, 100), identicalClients(4, 2000, 3, true), neutralNegativeCards(6), P1, P2, P3);
    }

    @Test
    void theBuyingPhaseMovesToTheNextPlayerInQueueOrder() {
        threePlayerSession();

        service.processSkipAction(ROOM, P1);

        GameSession session = service.getSession(ROOM);
        assertThat(session.getPhase()).isEqualTo(RoomPhase.BUYING);
        assertThat(session.getCurrentPlayerIndex()).isEqualTo(1);
    }

    @Test
    void eachBuyingTurnStartsAtCarSelection() {
        threePlayerSession();

        service.processSkipAction(ROOM, P1);

        assertThat(service.getSession(ROOM).getTurnStep()).isEqualTo(TurnStep.CAR_SELECTION);
    }

    @Test
    void theSellingPhaseStartsAfterTheLastPlayerHasBought() {
        threePlayerSession();

        service.processSkipAction(ROOM, P1);
        service.processSkipAction(ROOM, P2);
        service.processSkipAction(ROOM, P3);

        GameSession session = service.getSession(ROOM);
        assertThat(session.getPhase()).isEqualTo(RoomPhase.SELLING);
        assertThat(session.getTurnStep()).isEqualTo(TurnStep.CHOICE_CLIENT_TO_SELL);
    }

    @Test
    void theSellingPhaseStartsWithTheLastPlayerInTheQueue() {
        threePlayerSession();

        service.processSkipAction(ROOM, P1);
        service.processSkipAction(ROOM, P2);
        service.processSkipAction(ROOM, P3);

        assertThat(service.getSession(ROOM).getCurrentPlayerIndex()).isEqualTo(2);
    }

    @Test
    void theSellingPhaseMovesBackwardsThroughThePlayers() {
        threePlayerSession();
        service.processSkipAction(ROOM, P1);
        service.processSkipAction(ROOM, P2);
        service.processSkipAction(ROOM, P3);

        service.processSkipAction(ROOM, P3);

        assertThat(service.getSession(ROOM).getCurrentPlayerIndex()).isEqualTo(1);
    }

    @Test
    void aNewBuyingRoundStartsAfterTheFirstPlayerHasSold() {
        threePlayerSession();
        service.processSkipAction(ROOM, P1);
        service.processSkipAction(ROOM, P2);
        service.processSkipAction(ROOM, P3);
        service.processSkipAction(ROOM, P3);
        service.processSkipAction(ROOM, P2);

        service.processSkipAction(ROOM, P1);

        GameSession session = service.getSession(ROOM);
        assertThat(session.getPhase()).isEqualTo(RoomPhase.BUYING);
        assertThat(session.getCurrentPlayerIndex()).isZero();
        assertThat(session.getTurnStep()).isEqualTo(TurnStep.CAR_SELECTION);
    }

    @Test
    void aNewRoundReplacesTheCarsInTheShopWindow() {
        startSession(identicalCars(6, 100), identicalClients(6, 2000, 3, true), neutralNegativeCards(6), P1);
        List<Long> firstWindow = service.getSession(ROOM).getVisibleCars().stream().map(Car::getId).toList();

        service.processSkipAction(ROOM, P1);
        service.processSkipAction(ROOM, P1);

        assertThat(service.getSession(ROOM).getVisibleCars())
                .extracting(Car::getId)
                .doesNotContainAnyElementsOf(firstWindow);
    }

    @Test
    void buyingATuningPassesTheTurnToTheNextPlayer() {
        startSession(identicalCars(4, 100), identicalClients(4, 2000, 3, true),
                tuningPool(6, positiveTuning(1L, 200), positiveTuning(2L, 200),
                        positiveTuning(3L, 200), positiveTuning(4L, 200)),
                P1, P2, P3);
        service.processBuyCarAction(ROOM, P1, firstVisibleCarId());

        service.processBuyTuningAction(ROOM, P1, firstVisibleTuningId(), garageCarId(P1));

        assertThat(service.getSession(ROOM).getCurrentPlayerIndex()).isEqualTo(1);
    }

    @Test
    void skippingPassesTheTurnToTheNextPlayer() {
        threePlayerSession();

        service.processSkipAction(ROOM, P1);

        assertThat(service.getSession(ROOM).getCurrentPlayerIndex()).isEqualTo(1);
    }

    @Test
    void skippingOutOfTurnIsRejected() {
        threePlayerSession();

        assertThatThrownBy(() -> service.processSkipAction(ROOM, P2))
                .isInstanceOf(IllegalActionException.class);
    }

    @Test
    void aSessionStartsInTheBuyingPhaseAtCarSelection() {
        threePlayerSession();

        GameSession session = service.getSession(ROOM);
        assertThat(session.getPhase()).isEqualTo(RoomPhase.BUYING);
        assertThat(session.getTurnStep()).isEqualTo(TurnStep.CAR_SELECTION);
        assertThat(session.getCurrentPlayerIndex()).isZero();
    }

    @Test
    void theShopWindowHoldsOneCardMoreThanThePlayerCount() {
        threePlayerSession();

        assertThat(service.getSession(ROOM).getVisibleCars()).hasSize(4);
    }

    @Test
    void askingForASessionThatWasNeverStartedIsRejected() {
        assertThatThrownBy(() -> service.getSession(404L))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void aRemovedSessionIsGone() {
        threePlayerSession();

        service.removeSession(ROOM);

        assertThat(service.hasSession(ROOM)).isFalse();
    }

    @Test
    void startingTheSameRoomTwiceKeepsTheExistingSession() {
        GameSession first = startSession(identicalCars(4, 100), identicalClients(4, 2000, 3, true),
                neutralNegativeCards(6), P1, P2, P3);

        GameSession second = service.createSession(room(P1, P2, P3));

        assertThat(second).isSameAs(first);
    }
}
