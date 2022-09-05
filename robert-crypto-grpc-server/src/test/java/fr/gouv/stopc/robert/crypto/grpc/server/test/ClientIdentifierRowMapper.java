package fr.gouv.stopc.robert.crypto.grpc.server.test;

import fr.gouv.stopc.robert.crypto.grpc.server.storage.database.model.ClientIdentifier;
import org.springframework.jdbc.core.RowMapper;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

public class ClientIdentifierRowMapper implements RowMapper<ClientIdentifier> {

    @Override
    public ClientIdentifier mapRow(final ResultSet rs, final int rowNum) throws SQLException {
        return ClientIdentifier.builder()
                .id(rs.getLong("id"))
                .idA(rs.getString("idA"))
                .keyForMac(rs.getString("key_for_mac"))
                .keyForTuples(rs.getString("key_for_tuples"))
                .dateCreation(convertTimestampToDate(rs.getTimestamp("creation_time")))
                .dateMaj(convertTimestampToDate(rs.getTimestamp("last_update")))
                .build();
    }

    private Date convertTimestampToDate(final Timestamp timestamp) {
        return timestamp != null ? new Date(timestamp.getTime()) : null;
    }
}
