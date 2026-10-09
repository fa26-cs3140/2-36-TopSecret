import java.nio.file.Path;
import java.util.List;

/**
 * The only way the rest of the system reads mission briefs.
 * This uses Interface to connect separate parts of the system,
 */
public interface MissionStore extends MissionRepository, MissionSearch {

    // All missions ordered by id. Empty list if none.
    List<Mission> getAllMissions();

    // The mission with this database id, or null if there is none.
    Mission getMission(int id);


     // Missions whose brief contains the word or phrase, ignoring case.
     // A null or blank term, or no matches, gives an empty list.
    List<Mission> searchBriefs(String term);

    /**
      Imports missions from the tsv file. Records already stored
     *
     * @return the number of new records added
     * @throws IllegalStateException if the file cannot be read or the database fails
     */
    int importFromTsv(Path tsvFile);
}