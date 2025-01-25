
import com.example.myapplication.DatabaseConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet

class DatabaseHelper {

    suspend fun isAndroidIdRegistered(androidId: String): Boolean {
        val query = "SELECT COUNT(*) FROM fc_app_user_info WHERE android_id = ?"
        return withContext(Dispatchers.IO) {
            try {
                DriverManager.getConnection(
                    DatabaseConfig.JDBC_URL,
                    DatabaseConfig.DB_USER,
                    DatabaseConfig.DB_PASSWORD
                ).use { conn ->
                    conn.prepareStatement(query).use { stmt ->
                        stmt.setString(1, androidId)
                        stmt.executeQuery().use { rs ->
                            if (rs.next()) {
                                return@withContext rs.getInt(1) > 0
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            false
        }
    }

    fun getFireStationData(): List<String> {
        val resultList = mutableListOf<String>()
        var connection: Connection? = null

        try {
            Class.forName("com.mysql.cj.jdbc.Driver")
            connection = DriverManager.getConnection(
                DatabaseConfig.JDBC_URL,
                DatabaseConfig.DB_USER,
                DatabaseConfig.DB_PASSWORD
            )

            val query = "SELECT station_name FROM fc_firestation_Info"
            val statement = connection.createStatement()
            val resultSet: ResultSet = statement.executeQuery(query)

            while (resultSet.next()) {
                val stationName = resultSet.getString("station_name")
                resultList.add(stationName)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            connection?.close()
        }
        return resultList
    }
}
