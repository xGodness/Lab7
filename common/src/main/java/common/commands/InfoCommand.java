package common.commands;

import common.collection.CollectionManagerImpl;
import common.collectionexceptions.CollectionException;
import javax.validation.constraints.NotNull;

public class InfoCommand extends Command {
    public static final String tag = "info";
    public static final String description = "INFO ... provides information about collection";

    public InfoCommand() {
        super();
    }

    public static String getTag() {
        return tag;
    }

    public static String getDescription() {
        return description;
    }

    @Override
    public String execute(@NotNull CollectionManagerImpl moviesCollection, Object[] args, String username) throws CollectionException {
        if (moviesCollection.isEmpty()) throw new CollectionException("Collection is empty");
        return
                "Collection type     : " + moviesCollection.getCollection().getClass() + "\n" +
                "Initialization date : " + moviesCollection.getInitDateTime() + "\n" +
                "Collection size     : " + moviesCollection.getCollectionSize();
    }

}
