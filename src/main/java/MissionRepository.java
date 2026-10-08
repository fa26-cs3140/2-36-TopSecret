import java.util.List;

// Connects the UI to the database. Member A's database class implements this.
public interface MissionRepository {
    List<Mission> getAllMissions();
}