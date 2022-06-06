package server;

import common.commands.CommandImpl;
import server.database.DBManager;
import common.collectionexceptions.CollectionException;
import common.movieclasses.Movie;
import common.IO.IOManager;

import java.sql.SQLException;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * Class-connector. Operates all others classes presented in code.
 */


public class Application {
    private IOManager ioManager;
    private CollectionManager collectionManager;
    private CommandInvoker commandInvoker;
    private DBManager dbManager;

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
        System.out.println("DEBUG 4");
        LinkedList<Movie> collection = dbManager.getCollection();
        System.out.println("DEBUG 5");
        CollectionManager collectionManager = new CollectionManager(new ConcurrentSkipListSet(collection));
        System.out.println("DEBUG 6");
        collectionManager.setup(this);
        System.out.println("DEBUG 7");
        this.collectionManager = collectionManager;
        System.out.println("DEBUG 8");
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
     * @return        Execution result
     * @throws        CollectionException Exception thrown during command execution
     */
    public String executeCommand(CommandImpl command, Object[] args, String username) throws CollectionException {
        commandInvoker.setCommand(command, args, username);
        return commandInvoker.executeCommand();
    }

}
