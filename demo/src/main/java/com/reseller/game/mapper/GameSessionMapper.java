package com.reseller.game.mapper;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.reseller.game.dto.CarDto;
import com.reseller.game.dto.CurrentSaleDto;
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

        dto.setCars(session.getVisibleCars().stream()
                .map(carMapper::toDto)
                .collect(Collectors.toList()));

        dto.setClients(session.getVisibleClients().stream()
                .map(clientMapper::toDto)
                .collect(Collectors.toList()));

        dto.setTunings(session.getVisibleTunings().stream()
                .map(tuningMapper::toDto)
                .collect(Collectors.toList()));

        dto.setNegativeCards(session.getAvailableNegativeCards().stream()
                .map(tuningMapper::toDto)
                .collect(Collectors.toList()));

        dto.setCurrentPlayerIndex(session.getCurrentPlayerIndex());
        dto.setTurnStep(session.getTurnStep());
        dto.setPhase(session.getPhase());
        dto.setWinnerTelegramId(session.getWinnerTelegramId());
        dto.setCurrentSale(mapCurrentSale(session));

        dto.setPlayerQueue(mapPlayersWithGameState(session.getPlayers(), dto.getPlayerQueue()));

        return dto;
    }

    private CurrentSaleDto mapCurrentSale(GameSession session) {
        if (session.getCurrentSale() == null) return null;
        var s = session.getCurrentSale();
        return new CurrentSaleDto(
                s.getSellerTelegramId(),
                s.getClientId(),
                s.getCarInstanceId(),
                s.getDiceValue(),
                s.getThreshold(),
                s.getSuccess(),
                s.getProfit()
        );
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
                    playerDto.setSoldCars(gameState.getSoldCars());
                    playerDto.setTotalProfit(gameState.getTotalProfit());

                    List<CarDto> carDtos = gameState.getGarage().stream()
                            .map(this::mapCarInstance)
                            .collect(Collectors.toList());
                    playerDto.setCars(carDtos);

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

        dto.setNegativeCardRevealed(carInstance.isNegativeCardRevealed());
        if (carInstance.isNegativeCardRevealed() && carInstance.getHiddenNegativeCard() != null) {
            dto.setHiddenNegativeCard(tuningMapper.toDto(carInstance.getHiddenNegativeCard()));
        }

        return dto;
    }
}
