package com.reseller.game.mapper;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.reseller.game.dto.CarDto;
import com.reseller.game.dto.PlayerDto;
import com.reseller.game.dto.RoomStateDto;
import com.reseller.game.model.entity.GameRoom;
import com.reseller.game.session.CarInstance;
import com.reseller.game.session.GameSession;
import com.reseller.game.session.PlayerGameState;

/**
 * Maps GameSession (in-memory) + GameRoom (DB) -> RoomStateDto for client.
 * This combines persistent room data with temporary game session state.
 */
@Component
public class GameSessionMapper {

    private final CarMapper carMapper;
    private final ClientMapper clientMapper;
    private final TuningMapper tuningMapper;
    private final GameRoomMapper gameRoomMapper;
    private final SessionCardMapper sessionCardMapper;

    public GameSessionMapper(CarMapper carMapper, ClientMapper clientMapper,
                            TuningMapper tuningMapper, GameRoomMapper gameRoomMapper,
                            SessionCardMapper sessionCardMapper) {
        this.carMapper = carMapper;
        this.clientMapper = clientMapper;
        this.tuningMapper = tuningMapper;
        this.gameRoomMapper = gameRoomMapper;
        this.sessionCardMapper = sessionCardMapper;
    }

    /**
     * Map GameSession + GameRoom to RoomStateDto (for active games)
     */
    public RoomStateDto toDto(GameSession session, GameRoom room) {
        RoomStateDto dto = gameRoomMapper.toDto(room);

        dto.setCars(session.getVisibleCars().stream()
                .map(carMapper::toDto)
                .collect(Collectors.toList()));

        dto.setClients(session.getVisibleClients().stream()
                .map(clientMapper::toDto)
                .collect(Collectors.toList()));

        dto.setTunings(session.getVisibleTunings().stream()
                .map(tuningMapper::toDto)
                .collect(Collectors.toList()));

        dto.setCurrentPlayerIndex(session.getCurrentPlayerIndex());
        dto.setTurnStep(session.getTurnStep());
        dto.setPhase(session.getPhase());
        dto.setWinnerTelegramId(session.getWinnerTelegramId());
        dto.setCurrentSale(sessionCardMapper.toDto(session.getCurrentSale()));

        dto.setPlayerQueue(mapPlayersWithGameState(session.getPlayers(), dto.getPlayerQueue()));

        return dto;
    }

    /**
     * Map GameRoom only (for pending rooms before game starts)
     */
    public RoomStateDto toDtoFromRoom(GameRoom room) {
        return gameRoomMapper.toDto(room);
    }

    /**
     * Merge PlayerGameState (in-memory) with PlayerDto (from DB).
     * Session wins for every in-game number (balance, garage, soldCars, totalProfit) because those
     * count this match only; the DB DTO is kept just for the username, which the session copies.
     */
    private List<PlayerDto> mapPlayersWithGameState(List<PlayerGameState> gameStates, List<PlayerDto> playerDtos) {
        Map<String, PlayerDto> byTelegramId = playerDtos == null ? Map.of()
                : playerDtos.stream()
                        .collect(Collectors.toMap(PlayerDto::getTelegramId, Function.identity()));

        return gameStates.stream()
                .map(gameState -> {
                    PlayerDto playerDto = byTelegramId.getOrDefault(gameState.getTelegramId(), new PlayerDto());

                    playerDto.setTelegramId(gameState.getTelegramId());
                    if (playerDto.getUsername() == null) {
                        playerDto.setUsername(gameState.getUsername());
                    }
                    playerDto.setBalance(gameState.getBalance());
                    playerDto.setGarageSize(gameState.getGarageCapacity());
                    playerDto.setSoldCars(gameState.getSoldCars());
                    playerDto.setTotalProfit(gameState.getTotalProfit());

                    playerDto.setCars(gameState.getGarage().stream()
                            .map(this::mapCarInstance)
                            .collect(Collectors.toList()));

                    return playerDto;
                })
                .collect(Collectors.toList());
    }

    /** Fills the secret card only once revealed - before that the client must not see it. */
    private CarDto mapCarInstance(CarInstance carInstance) {
        CarDto dto = sessionCardMapper.toDto(carInstance);
        if (carInstance.isNegativeCardRevealed() && carInstance.getHiddenNegativeCard() != null) {
            dto.setHiddenNegativeCard(tuningMapper.toDto(carInstance.getHiddenNegativeCard()));
        }
        return dto;
    }
}
