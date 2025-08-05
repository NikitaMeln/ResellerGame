package com.reseller.game.mapper;

import com.reseller.game.dto.RoomStateDto;
import com.reseller.game.model.entity.Car;
import com.reseller.game.model.entity.Client;
import com.reseller.game.model.entity.GameRoom;
import com.reseller.game.model.entity.Player;
import com.reseller.game.model.entity.Tuning;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-08-05T21:09:22+0200",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.42.50.v20250729-0351, environment: Java 21.0.8 (Eclipse Adoptium)"
)
@Component
public class GameRoomMapperImpl implements GameRoomMapper {

    @Override
    public RoomStateDto toDto(GameRoom room) {
        if ( room == null ) {
            return null;
        }

        RoomStateDto roomStateDto = new RoomStateDto();

        roomStateDto.setRoomState( stateToString( room.getState() ) );
        List<Player> list = room.getPlayerQueue();
        if ( list != null ) {
            roomStateDto.setPlayerQueue( new ArrayList<Player>( list ) );
        }
        List<Car> list1 = room.getCars();
        if ( list1 != null ) {
            roomStateDto.setCars( new ArrayList<Car>( list1 ) );
        }
        List<Client> list2 = room.getClients();
        if ( list2 != null ) {
            roomStateDto.setClients( new ArrayList<Client>( list2 ) );
        }
        roomStateDto.setPhase( room.getPhase() );
        roomStateDto.setStartTime( room.getStartTime() );
        List<Tuning> list3 = room.getTunings();
        if ( list3 != null ) {
            roomStateDto.setTunings( new ArrayList<Tuning>( list3 ) );
        }

        return roomStateDto;
    }

    @Override
    public GameRoom toEntity(RoomStateDto dto) {
        if ( dto == null ) {
            return null;
        }

        GameRoom.GameRoomBuilder gameRoom = GameRoom.builder();

        gameRoom.state( stringToState( dto.getRoomState() ) );
        List<Player> list = dto.getPlayerQueue();
        if ( list != null ) {
            gameRoom.playerQueue( new ArrayList<Player>( list ) );
        }
        List<Client> list1 = dto.getClients();
        if ( list1 != null ) {
            gameRoom.clients( new ArrayList<Client>( list1 ) );
        }
        List<Car> list2 = dto.getCars();
        if ( list2 != null ) {
            gameRoom.cars( new ArrayList<Car>( list2 ) );
        }
        List<Tuning> list3 = dto.getTunings();
        if ( list3 != null ) {
            gameRoom.tunings( new ArrayList<Tuning>( list3 ) );
        }
        gameRoom.startTime( dto.getStartTime() );
        gameRoom.phase( dto.getPhase() );

        return gameRoom.build();
    }
}
