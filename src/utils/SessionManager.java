package utils;

import dao.UserDAO;
import model.User;

public class SessionManager {
    private static User currentUser;

    public static void setCurrentUser(User user) {
        currentUser = user;
    }

    public static User getCurrentUser() {
        return currentUser;
    }

    public static String getCurrentUsername() {
        return currentUser != null ? currentUser.getUsername() : null; // Re-added
    }

    public static String getCurrentRole() {
        return currentUser != null ? currentUser.getRole() : null;
    }

    public static int getCurrentUserId() {
        return currentUser != null ? currentUser.getId() : -1;
    }

    public static boolean isLoggedIn() {
        return currentUser != null;
    }

    public static boolean isManager() {
        return currentUser != null && currentUser.getRole().equals("manager");
    }

    public static void setOnlineStatus() {
        if (currentUser != null) {
            UserDAO userDAO = new UserDAO();
            userDAO.updateStatus(currentUser.getId(), "online");
            currentUser.setStatus("online");
        }
    }

    public static void setOfflineStatus() {
        if (currentUser != null) {
            UserDAO userDAO = new UserDAO();
            userDAO.updateStatus(currentUser.getId(), "offline");
            currentUser.setStatus("offline");
        }
    }

    public static void logout() {
        setOfflineStatus();
        currentUser = null;
    }
}