package session;

/**
 * Holds information about the currently logged-in user for the
 * lifetime of the running application.
 *
 * Session.userId is used as pharmacist_id in the bills table
 * whenever a bill is finalized (both prescription and
 * non-prescription sales).
 */
public class Session {
    public static int userId;
    public static String username;
    public static String role;

    /** Clears the session, used on logout. */
    public static void clear() {
        userId = 0;
        username = null;
        role = null;
    }
}
