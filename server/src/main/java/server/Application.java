package server;

import common.commands.CommandImpl;
import server.database.DBManager;
import common.collectionexceptions.CollectionException;
import common.movieclasses.Movie;
import common.IO.IOManager;

import java.sql.SQLException;
import java.util.LinkedList;

/**
 * Class-connector. Operates all others classes presented in code.
 */


public class Application {
    private IOManager ioManager;
    private CollectionManager collectionManager;
    private CommandInvoker commandInvoker;
    private DBManager dbManager;

    private boolean collectionWasLoaded = false;

    /*________________________________________________________________________________________________________________
                                                    Constructor
    ________________________________________________________________________________________________________________*/

    public Application(DBManager dbManager) {
        this.ioManager = new IOManager();
        this.collectionManager = null;
        this.commandInvoker = new CommandInvoker(this);
        this.dbManager = dbManager;
    }

    /*________________________________________________________________________________________________________________
                                                    Load collection
    ________________________________________________________________________________________________________________*/

    /**
     * Loads collection from the database.
     *
     */
    public void loadCollection() throws SQLException {
        LinkedList<Movie> collection = dbManager.getCollection();
        CollectionManager collectionManager = new CollectionManager(collection);
        collectionManager.setup(this);
        this.collectionManager = collectionManager;
        collectionWasLoaded = true;
    }

    /*________________________________________________________________________________________________________________
                                                        Getters
    ________________________________________________________________________________________________________________*/

    /**
     * @return Application's IOManager
     */
    public IOManager getIoManager() {
        return ioManager;
    }

    /**
     * @return Application's CollectionManager
     */
    public CollectionManager getCollectionManager() {
        return collectionManager;
    }

    public DBManager getDbManager() {
        return dbManager;
    }


    /*________________________________________________________________________________________________________________
                                                   Execute command
    ________________________________________________________________________________________________________________*/

    /**
     * Sets command to invoker and makes it to execute.
     *
     * @param command Command to execute
     * @param args    Command arguments (necessary, can be null)
     * @return Execution result
     * @throws CollectionException Exception thrown during executing the command
     */
    public String executeCommand(CommandImpl command, Object[] args, String username) throws CollectionException {
        commandInvoker.setCommand(command, args, username);
        return commandInvoker.executeCommand();
    }

}
