package com.reseller.game.mapper;

import java.util.List;
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

    public GameSessionMapper(CarMapper carMapper, ClientMapper clientMapper,
                            TuningMapper tuningMapper, GameRoomMapper gameRoomMapper) {
        this.carMapper = carMapper;
        this.clientMapper = clientMapper;
        this.tuningMapper = tuningMapper;
        this.gameRoomMapper = gameRoomMapper;
    }

    /**
     * Map GameSession + GameRoom to RoomStateDto (for active games)
     */
    public RoomStateDto toDto(GameSession session, GameRoom room) {
        RoomStateDto dto = gameRoomMapper.toDto(room);

        dto.setCars(session.getAvailableCars().stream()
                .map(carMapper::toDto)
                .collect(Collectors.toList()));

        dto.setClients(session.getAvailableClients().stream()
                .map(clientMapper::toDto)
                .collect(Collectors.toList()));

        dto.setTunings(session.getAvailableTunings().stream()
                .map(tuningMapper::toDto)
                .collect(Collectors.toList()));

        dto.setNegativeCards(session.getAvailableNegativeCards().stream()
                .map(tuningMapper::toDto)
                .collect(Collectors.toList()));

        dto.setCurrentPlayerIndex(session.getCurrentPlayerIndex());
        dto.setTurnStep(session.getTurnStep());

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
     * Merge PlayerGameState (in-memory) with PlayerDto (from DB)
     */
    private List<PlayerDto> mapPlayersWithGameState(List<PlayerGameState> gameStates, List<PlayerDto> playerDtos) {
        return gameStates.stream()
                .map(gameState -> {
                    PlayerDto playerDto = playerDtos.stream()
                            .filter(p -> p.getTelegramId().equals(gameState.getTelegramId()))
                            .findFirst()
                            .orElse(new PlayerDto());

                    playerDto.setTelegramId(gameState.getTelegramId());
                    playerDto.setUsername(gameState.getUsername());
                    playerDto.setBalance(gameState.getBalance());
                    playerDto.setGarageSize(gameState.getGarageCapacity());

                    List<CarDto> carDtos = gameState.getGarage().stream()
                            .map(this::mapCarInstance)
                            .collect(Collectors.toList());
                    playerDto.setCars(carDtos);

                    if (gameState.getCurrentNegativeCard() != null) {
                        playerDto.setCurrentNegativeCard(tuningMapper.toDto(gameState.getCurrentNegativeCard()));
                    }

                    return playerDto;
                })
                .collect(Collectors.toList());
    }

    /**
     * Map CarInstance (in-memory) to CarDto
     */
    private CarDto mapCarInstance(CarInstance carInstance) {
        CarDto dto = new CarDto();
        dto.setId(carInstance.getOriginalCarId());
        dto.setInstanceId(carInstance.getInstanceId());
        dto.setModel(carInstance.getModel());
        dto.setYear(carInstance.getYear());
        dto.setPrice(carInstance.getBasePrice());

        dto.setTuning(carInstance.getAppliedTunings().stream()
                .map(tuningMapper::toDto)
                .collect(Collectors.toList()));

        return dto;
    }
}
