package common.commands;

import common.collection.CollectionManagerImpl;
import common.databaseexceptions.DatabaseException;
import common.collectionexceptions.CollectionException;

import javax.validation.constraints.NotNull;

import java.sql.SQLException;

public class RemoveHeadCommand extends Command {
    public static final String tag = "remove_head";
    public static final String description = "REMOVE_HEAD ... shows and removes first element in the collection owned by current user";

    public RemoveHeadCommand() {
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
        try {
            return moviesCollection.removeHead(username).toString();
        } catch (DatabaseException e) {
            throw new CollectionException(e.getMessage());
        } catch (SQLException e) {
            e.printStackTrace();
            throw new CollectionException("Error while connecting to the database");
        }
    }

}
