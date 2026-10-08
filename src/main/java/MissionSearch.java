import java.util.List;
//This helps the mission search for functionality and it connects the UI with the search implementation
public interface MissionSearch {
    List<Mission> searchBriefs(String query);
}