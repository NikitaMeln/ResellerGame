package com.reseller.game.service.impl;

import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.mockito.InjectMocks;
import org.mockito.Mock;

import com.reseller.game.model.entity.Car;
import com.reseller.game.model.entity.Client;
import com.reseller.game.model.entity.GameRoom;
import com.reseller.game.model.entity.Player;
import com.reseller.game.model.entity.Tuning;
import com.reseller.game.model.entity.types.TuningType;
import com.reseller.game.repository.CarRepository;
import com.reseller.game.repository.ClientRepository;
import com.reseller.game.repository.TuningRepository;
import com.reseller.game.session.GameSession;

/**
 * Shared fixture for GameSessionServiceImpl tests.
 *
 * The session shuffles every pool on creation, so tests stay deterministic by either supplying
 * exactly (playerCount + 1) cards - the whole pool then lands in the visible window - or by
 * making every card of a kind identical apart from its id and reading the ids back from the
 * session instead of assuming them.
 */
abstract class GameSessionTestSupport {

    static final long ROOM = 1L;
    static final int STARTING_BALANCE = 1300;
    static final String P1 = "p1";
    static final String P2 = "p2";
    static final String P3 = "p3";

    @Mock
    CarRepository carRepository;
    @Mock
    ClientRepository clientRepository;
    @Mock
    TuningRepository tuningRepository;

    @InjectMocks
    GameSessionServiceImpl service;

    GameSession startSession(List<Car> cars, List<Client> clients, List<Tuning> tunings, String... playerIds) {
        when(carRepository.findAll()).thenReturn(new ArrayList<>(cars));
        when(clientRepository.findAll()).thenReturn(new ArrayList<>(clients));
        when(tuningRepository.findAll()).thenReturn(new ArrayList<>(tunings));
        return service.createSession(room(playerIds));
    }

    static GameRoom room(String... playerIds) {
        List<Player> queue = new ArrayList<>();
        for (String id : playerIds) {
            Player p = new Player();
            p.setTelegramId(id);
            p.setUsername("name-" + id);
            queue.add(p);
        }
        return GameRoom.builder().id(ROOM).players(new ArrayList<>(queue)).playerQueue(queue).build();
    }

    static Car car(long id, int price) {
        return car(id, price, "1995");
    }

    static Car car(long id, int price, String year) {
        return Car.builder().id(id).model("model-" + id).year(year).price(BigDecimal.valueOf(price)).build();
    }

    static List<Car> identicalCars(int count, int price) {
        List<Car> cars = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            cars.add(Car.builder().id((long) i).model("model").year("1995").price(BigDecimal.valueOf(price)).build());
        }
        return cars;
    }

    /** Client that accepts a stock 1990s car and needs a roll of at least randomCounter. */
    static Client client(long id, int budget, int randomCounter, boolean wantsStock) {
        return client(id, budget, randomCounter, wantsStock, "1990-x");
    }

    static Client client(long id, int budget, int randomCounter, boolean wantsStock, String decade) {
        return Client.builder()
                .id(id)
                .name("client-" + id)
                .budget(BigDecimal.valueOf(budget))
                .yearForPurchase(decade)
                .randomCounter(randomCounter)
                .stockOrNot(wantsStock)
                .build();
    }

    static List<Client> identicalClients(int count, int budget, int randomCounter, boolean wantsStock) {
        List<Client> clients = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            clients.add(Client.builder()
                    .id((long) i)
                    .name("client")
                    .budget(BigDecimal.valueOf(budget))
                    .yearForPurchase("1990-x")
                    .randomCounter(randomCounter)
                    .stockOrNot(wantsStock)
                    .build());
        }
        return clients;
    }

    /** Tuning with no addToRandomModifier, so it never shifts the dice threshold. */
    static Tuning positiveTuning(long id, int price) {
        return Tuning.builder()
                .id(id)
                .name("tuning-" + id)
                .price(BigDecimal.valueOf(price))
                .type(TuningType.POSITIVE)
                .properties(new HashMap<>())
                .build();
    }

    /**
     * Two twins share every field Tuning.equals looks at (name, price, type, properties) but have
     * different ids - the shape that makes CarInstance.addTuning treat the second one as a duplicate.
     */
    static Tuning twinTuning(long id, int price) {
        return Tuning.builder()
                .id(id)
                .name("twin")
                .price(BigDecimal.valueOf(price))
                .type(TuningType.POSITIVE)
                .properties(new HashMap<>())
                .build();
    }

    static Tuning positiveTuningWithModifier(long id, int price, int addToRandomModifier) {
        HashMap<String, Object> properties = new HashMap<>();
        properties.put("addToRandomModifier", addToRandomModifier);
        return Tuning.builder()
                .id(id)
                .name("tuning-" + id)
                .price(BigDecimal.valueOf(price))
                .type(TuningType.POSITIVE)
                .properties(properties)
                .build();
    }

    /** All cards carry the same modifier, so it does not matter which one the session draws. */
    static List<Tuning> negativeCardsWithModifier(int count, int addToRandomModifier) {
        List<Tuning> cards = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            HashMap<String, Object> properties = new HashMap<>();
            properties.put("addToRandomModifier", addToRandomModifier);
            cards.add(Tuning.builder()
                    .id(1000L + i)
                    .name("negative-" + i)
                    .price(BigDecimal.ZERO)
                    .type(TuningType.NEGATIVE)
                    .properties(properties)
                    .build());
        }
        return cards;
    }

    /** Hidden card with no addToRandomModifier, so the threshold stays equal to client.randomCounter. */
    static List<Tuning> neutralNegativeCards(int count) {
        List<Tuning> cards = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            cards.add(Tuning.builder()
                    .id(1000L + i)
                    .name("negative-" + i)
                    .price(BigDecimal.ZERO)
                    .type(TuningType.NEGATIVE)
                    .properties(new HashMap<>())
                    .build());
        }
        return cards;
    }

    /** Full tuning pool as the service expects it: negatives feed secret cards, positives the shop window. */
    static List<Tuning> tuningPool(int negativeCount, Tuning... positives) {
        List<Tuning> all = new ArrayList<>(neutralNegativeCards(negativeCount));
        all.addAll(List.of(positives));
        return all;
    }

    long firstVisibleCarId() {
        return service.getSession(ROOM).getVisibleCars().get(0).getId();
    }

    long firstVisibleClientId() {
        return service.getSession(ROOM).getVisibleClients().get(0).getId();
    }

    long firstVisibleTuningId() {
        return service.getSession(ROOM).getVisibleTunings().get(0).getId();
    }

    String garageCarId(String player) {
        return service.getSession(ROOM).findPlayer(player).getGarage().get(0).getInstanceId();
    }

    int balanceOf(String player) {
        return service.getSession(ROOM).findPlayer(player).getBalance();
    }

    /**
     * Buys the first visible car, skips tuning and picks the first visible client, leaving the
     * session in TURN_MULTIPLIER with the secret card already revealed. Single-player only:
     * one skip is enough to flip BUYING into SELLING.
     */
    void playUntilDiceRoll(String player) {
        service.processBuyCarAction(ROOM, player, firstVisibleCarId());
        service.processSkipAction(ROOM, player);
        service.processChooseClientAndCarAction(ROOM, player, firstVisibleClientId(), garageCarId(player));
        service.processRevealSecretCardAction(ROOM, player);
    }

    /** One full buy-and-sell round that always succeeds (threshold 3, dice 6). Single-player only. */
    void playWinningRound(String player) {
        playUntilDiceRoll(player);
        service.processRollDiceAction(ROOM, player, 6);
        service.processNextTurnAction(ROOM, player);
    }
}
