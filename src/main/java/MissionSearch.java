import java.util.List;
//This helps the mission search for functionality as it connects the UI with the search implementation
public interface MissionSearch {
    List<Mission> searchBriefs(String query);
}