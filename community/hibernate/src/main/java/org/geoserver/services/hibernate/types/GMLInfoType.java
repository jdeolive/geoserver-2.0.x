
package org.geoserver.services.hibernate.types;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import java.util.logging.Logger;
import org.geoserver.services.hibernate.beans.GMLInfoImplHb;
import org.geoserver.wfs.GMLInfo;
import org.geotools.util.Utilities;
import org.geotools.util.logging.Logging;
import org.hibernate.HibernateException;
import org.hibernate.usertype.UserType;

/**
* Hibernate user type for {@link GMLInfo}.
*/
public class GMLInfoType implements UserType {

   private static final Logger LOGGER = Logging.getLogger(GMLInfoType.class);

   public Object assemble(Serializable cached, Object owner) throws HibernateException {
       return cached;
   }

   public Object deepCopy(Object value) throws HibernateException {
       return value;
   }

   public Serializable disassemble(Object value) throws HibernateException {
       return (Serializable) value;
   }

   public boolean equals(Object x, Object y) throws HibernateException {
       return Utilities.equals(x, y);
   }

   public int hashCode(Object x) throws HibernateException {
       return Utilities.deepHashCode(x);
   }

   public boolean isMutable() {
       return false;
   }

   public Object nullSafeGet(ResultSet rs, String[] names, Object owner)
           throws HibernateException, SQLException {

       String srsNameStyle = rs.getString(names[0]);
       GMLInfoImplHb gmlInfo = new GMLInfoImplHb();
       gmlInfo.setSrsNameStyle(GMLInfo.SrsNameStyle.valueOf(srsNameStyle));
       return gmlInfo;
   }

   public void nullSafeSet(PreparedStatement st, Object value, int index)
           throws HibernateException, SQLException {

       GMLInfoImplHb gmlInfo = (GMLInfoImplHb)value;

       if (gmlInfo == null) {
           st.setString(index, GMLInfo.SrsNameStyle.NORMAL.name());
       } else {
           st.setString(index, gmlInfo.getSrsNameStyle().name());
       }
   }

   public Object replace(Object original, Object target, Object owner) throws HibernateException {
       return original;
   }

   public Class<?> returnedClass() {
       return GMLInfoImplHb.class;
   }

   private static final int[] SQLTYPES = new int[] { Types.VARCHAR};
   public int[] sqlTypes() {
       return SQLTYPES;
   }
}
