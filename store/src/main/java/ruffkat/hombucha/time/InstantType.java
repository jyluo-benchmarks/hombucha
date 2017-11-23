package ruffkat.hombucha.time;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.usertype.UserType;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;

/**
 * Persists instances of {@code JSR-310} {@link java.time.Instant}
 */
public class InstantType
        implements UserType {

    public Class returnedClass() {
        return Instant.class;
    }

    public boolean equals(Object x, Object y)
            throws HibernateException {
        return x != null ? x.equals(y) : y == x;
    }

    public int hashCode(Object x)
            throws HibernateException {
        return x == null ? 0 : x.hashCode();
    }

    public int[] sqlTypes() {
        return new int[]{Types.TIMESTAMP};
    }

    @Override
    public void nullSafeSet(PreparedStatement statement, Object value, int index, SharedSessionContractImplementor implementor)
            throws HibernateException, SQLException {
        if (value == null) {
            statement.setNull(index, Types.TIMESTAMP);
        } else {
            Instant instant = (Instant) value;
            Timestamp timestamp = new Timestamp(instant.toEpochMilli());
            statement.setObject(index, timestamp, Types.TIMESTAMP);
        }
    }

    @Override
    public Object nullSafeGet(ResultSet resultSet, String[] names, SharedSessionContractImplementor implementor, Object owner)
            throws HibernateException, SQLException {
        if (!resultSet.wasNull()) {
            Timestamp timestamp = resultSet.getTimestamp(names[0]);
            if (timestamp != null) {
                return Instant.ofEpochMilli(timestamp.getTime());
            }
        }
        return null;
    }

    public Object deepCopy(Object value)
            throws HibernateException {
        return value;
    }

    public boolean isMutable() {
        return false;
    }

    public Serializable disassemble(Object value)
            throws HibernateException {
        return (Serializable) value;
    }

    public Object assemble(Serializable cached, Object owner)
            throws HibernateException {
        return cached;
    }

    public Object replace(Object original, Object target, Object owner)
            throws HibernateException {
        return original;
    }
}