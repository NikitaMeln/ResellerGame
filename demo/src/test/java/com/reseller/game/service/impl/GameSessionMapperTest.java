package com.reseller.game.service.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.reseller.game.dto.PlayerDto;
import com.reseller.game.dto.RoomStateDto;
import com.reseller.game.mapper.CarMapperImpl;
import com.reseller.game.mapper.ClientMapperImpl;
import com.reseller.game.mapper.GameRoomMapperImpl;
import com.reseller.game.mapper.GameSessionMapper;
import com.reseller.game.mapper.PlayerMapperImpl;
import com.reseller.game.mapper.SessionCardMapperImpl;
import com.reseller.game.mapper.TuningMapperImpl;
import com.reseller.game.model.entity.GameRoom;
import com.reseller.game.model.entity.types.RoomState;
import com.reseller.game.session.GameSession;

/**
 * What the broadcast DTO exposes. Lives in this package to reuse GameSessionTestSupport, which
 * drives a real session through the service instead of hand-building one.
 */
@ExtendWith(MockitoExtension.class)
class GameSessionMapperTest extends GameSessionTestSupport {

    private final TuningMapperImpl tuningMapper = new TuningMapperImpl();

    private final GameSessionMapper mapper = new GameSessionMapper(
            new CarMapperImpl(),
            new ClientMapperImpl(),
            tuningMapper,
            new GameRoomMapperImpl(new PlayerMapperImpl()),
            new SessionCardMapperImpl(tuningMapper));

    private RoomStateDto map(GameSession session) {
        GameRoom room = room(P1);
        room.setState(RoomState.STARTED);
        return mapper.toDto(session, room);
    }

    @Test
    void theShopWindowIsMappedRatherThanTheWholePool() {
        GameSession session = startSession(
                identicalCars(10, 500), identicalClients(10, 2000, 3, true),
                tuningPool(5, positiveTuning(1L, 100), positiveTuning(2L, 100), positiveTuning(3L, 100)),
                P1);

        RoomStateDto dto = map(session);

        assertThat(dto.getCars()).hasSameSizeAs(session.getVisibleCars());
        assertThat(dto.getClients()).hasSameSizeAs(session.getVisibleClients());
        assertThat(dto.getTunings()).hasSameSizeAs(session.getVisibleTunings());
        assertThat(dto.getCars()).hasSize(2);
    }

    @Test
    void inGameNumbersComeFromTheSessionNotTheCareerRecord() {
        GameSession session = startSession(
                identicalCars(2, 500), identicalClients(2, 2000, 3, true),
                neutralNegativeCards(3), P1);
        playWinningRound(P1);

        PlayerDto player = map(session).getPlayerQueue().get(0);

        assertThat(player.getSoldCars()).isEqualTo(1);
        assertThat(player.getTotalProfit()).isEqualTo(2000);
        assertThat(player.getBalance()).isEqualTo(STARTING_BALANCE - 500 + 2000);
        assertThat(player.getGarageSize()).isEqualTo(3);
    }

    @Test
    void theSecretCardStaysHiddenUntilItIsRevealed() {
        GameSession session = startSession(
                identicalCars(2, 500), identicalClients(2, 2000, 3, true),
                neutralNegativeCards(3), P1);
        service.processBuyCarAction(ROOM, P1, firstVisibleCarId());

        PlayerDto beforeReveal = map(session).getPlayerQueue().get(0);
        assertThat(beforeReveal.getCars().get(0).isNegativeCardRevealed()).isFalse();
        assertThat(beforeReveal.getCars().get(0).getHiddenNegativeCard()).isNull();

        service.processSkipAction(ROOM, P1);
        service.processChooseClientAndCarAction(ROOM, P1, firstVisibleClientId(), garageCarId(P1));
        service.processRevealSecretCardAction(ROOM, P1);

        PlayerDto afterReveal = map(session).getPlayerQueue().get(0);
        assertThat(afterReveal.getCars().get(0).isNegativeCardRevealed()).isTrue();
        assertThat(afterReveal.getCars().get(0).getHiddenNegativeCard()).isNotNull();
    }

    @Test
    void aCarInTheGarageKeepsItsIdentityAndAppliedTunings() {
        GameSession session = startSession(
                identicalCars(2, 500), identicalClients(2, 2000, 3, true),
                tuningPool(3, positiveTuning(1L, 200)), P1);
        service.processBuyCarAction(ROOM, P1, firstVisibleCarId());
        service.processBuyTuningAction(ROOM, P1, firstVisibleTuningId(), garageCarId(P1));

        var car = map(session).getPlayerQueue().get(0).getCars().get(0);

        assertThat(car.getInstanceId()).isEqualTo(garageCarId(P1));
        assertThat(car.getId()).isNotNull();
        assertThat(car.getPrice()).isEqualByComparingTo("500");
        assertThat(car.getModel()).isEqualTo("model");
        assertThat(car.getTuning()).extracting("price").containsExactly(new java.math.BigDecimal("200"));
    }

    @Test
    void currentSaleIsAbsentBeforeASaleAndCarriesTheRollAfterwards() {
        GameSession session = startSession(
                identicalCars(2, 500), identicalClients(2, 2000, 3, true),
                neutralNegativeCards(3), P1);

        assertThat(map(session).getCurrentSale()).isNull();

        playUntilDiceRoll(P1);
        var pending = map(session).getCurrentSale();
        assertThat(pending.getSellerTelegramId()).isEqualTo(P1);
        assertThat(pending.getDiceValue()).isNull();
        assertThat(pending.getSuccess()).isNull();

        service.processRollDiceAction(ROOM, P1, 6);
        var rolled = map(session).getCurrentSale();
        assertThat(rolled.getDiceValue()).isEqualTo(6);
        assertThat(rolled.getThreshold()).isEqualTo(3);
        assertThat(rolled.getSuccess()).isTrue();
        assertThat(rolled.getProfit()).isEqualTo(2000);
    }

    @Test
    void turnOrderIsPreservedInThePlayerQueue() {
        GameSession session = startSession(
                identicalCars(4, 500), identicalClients(4, 2000, 3, true),
                neutralNegativeCards(5), P1, P2, P3);

        RoomStateDto dto = map(session);

        assertThat(dto.getPlayerQueue()).extracting(PlayerDto::getTelegramId)
                .containsExactly(P1, P2, P3);
        assertThat(dto.getCurrentPlayerIndex()).isZero();
    }

    @Test
    void aPendingRoomIsMappedWithoutAnyGameState() {
        GameRoom room = room(P1, P2);
        room.setState(RoomState.PENDING);

        RoomStateDto dto = mapper.toDtoFromRoom(room);

        assertThat(dto.getRoomState()).isEqualTo("PENDING");
        assertThat(dto.getPlayerQueue()).extracting(PlayerDto::getTelegramId).containsExactly(P1, P2);
        assertThat(dto.getPlayerQueue()).extracting(PlayerDto::getBalance).containsOnlyNulls();
        assertThat(dto.getCars()).isNull();
        assertThat(dto.getCurrentSale()).isNull();
    }

    @Test
    void mappingTheSameSessionTwiceDoesNotAccumulateCars() {
        GameSession session = startSession(
                identicalCars(2, 500), identicalClients(2, 2000, 3, true),
                neutralNegativeCards(3), P1);
        service.processBuyCarAction(ROOM, P1, firstVisibleCarId());

        List<PlayerDto> first = map(session).getPlayerQueue();
        List<PlayerDto> second = map(session).getPlayerQueue();

        assertThat(first.get(0).getCars()).hasSize(1);
        assertThat(second.get(0).getCars()).hasSize(1);
    }
}
