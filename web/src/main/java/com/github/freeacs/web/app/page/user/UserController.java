package com.github.freeacs.web.app.page.user;

import com.github.freeacs.dbi.User;
import com.github.freeacs.dbi.Users;
import com.github.freeacs.web.app.util.SessionCache;
import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

/** The Class UserController. */
@Controller
@RequestMapping("/app/user")
public class UserController extends PermissionController {
  /** The user group controller. */
  @Autowired UserGroupController userGroupController;

  @Autowired
  @Qualifier("main")
  DataSource mainDataSource;

  /**
   * Delete.
   *
   * @param name the name
   * @param session the session
   * @throws IOException Signals that an I/O exception has occurred.
   * @throws ParseException the parse exception
   * @throws SQLException the sQL exception the no available connection exception
   */
  @RequestMapping(value = "/{name}", method = RequestMethod.DELETE)
  @ResponseBody
  public void delete(@PathVariable String name, HttpSession session)
      throws IOException, ParseException, SQLException {
    Users users = getUsers(session.getId(), mainDataSource);
    User loggedInUser = SessionCache.getSessionData(session.getId()).getUser();
    if (users.getProtected(name, loggedInUser) == null) {
      throw new ResourceNotFoundException();
    }
    users.delete(users.getProtected(name, loggedInUser), loggedInUser);
  }

  /**
   * Gets the.
   *
   * @param name the name
   * @param session the session
   * @return the user model
   * @throws IOException Signals that an I/O exception has occurred.
   * @throws SQLException the sQL exception the no available connection exception
   */
  @RequestMapping(value = "/{name}", method = RequestMethod.GET)
  @ResponseBody
  public UserModel get(@PathVariable String name, HttpSession session)
      throws IOException, SQLException {
    Users users = getUsers(session.getId(), mainDataSource);
    User loggedInUser = SessionCache.getSessionData(session.getId()).getUser();
    if (users.getProtected(name, loggedInUser) == null) {
      throw new ResourceNotFoundException();
    }
    return UserModel.fromUser(
        users.getProtected(name, loggedInUser), userGroupController.getNameMap().get("NotAdmin"));
  }

  /**
   * Creates the.
   *
   * @param details the details
   * @param session the session
   * @return the user model
   * @throws IOException Signals that an I/O exception has occurred.
   * @throws ParseException the parse exception
   * @throws SQLException the sQL exception the no available connection exception
   */
  @RequestMapping(method = RequestMethod.POST)
  @ResponseBody
  public UserModel create(@RequestBody UserModel details, HttpSession session)
      throws IOException, ParseException, SQLException {
    Users users = getUsers(session.getId(), mainDataSource);
    User loggedInUser = SessionCache.getSessionData(session.getId()).getUser();
    if (!details.getUsername().isEmpty() && users.getUnprotected(details.getUsername()) == null) {
      User toCreate = UserModel.toUser(details, users);
      users.addOrChange(toCreate, loggedInUser);
      return UserModel.fromUser(toCreate, userGroupController.getNameMap().get("NotAdmin"));
    } else {
      throw new NotAllowedException("A user exists with that username");
    }
  }

  /**
   * Update.
   *
   * @param details the details
   * @param session the session
   * @return the user model
   * @throws IOException Signals that an I/O exception has occurred.
   * @throws ParseException the parse exception
   * @throws SQLException the sQL exception the no available connection exception
   */
  @RequestMapping(method = RequestMethod.PUT)
  @ResponseBody
  public UserModel update(@RequestBody UserModel details, HttpSession session)
      throws IOException, ParseException, SQLException {
    Users users = getUsers(session.getId(), mainDataSource);
    User loggedInUser = SessionCache.getSessionData(session.getId()).getUser();
    if (users.getProtected(details.getUsername(), loggedInUser) != null) {
      User oldUser = users.getProtected(details.getUsername(), loggedInUser);
      oldUser.setAccess(details.getAccess());
      oldUser.setFullname(details.getFullname());
      oldUser.setUsername(details.getUsername());
      oldUser.setSecretHashed(details.getPassword());
      users.addOrChange(oldUser, loggedInUser);
      return UserModel.fromUser(oldUser, userGroupController.getNameMap().get("NotAdmin"));
    } else {
      throw new ResourceNotFoundException();
    }
  }

  /**
   * List.
   *
   * @param session the session
   * @param request the request
   * @param outputHandler the outputHandler
   * @return the map
   * @throws IOException Signals that an I/O exception has occurred.
   * @throws ParseException the parse exception
   * @throws SQLException the sQL exception the no available connection exception
   */
  @RequestMapping(value = "list", method = RequestMethod.GET)
  @ResponseBody
  public Map<String, Object> list(
      HttpSession session, HttpServletRequest request, HttpServletResponse outputHandler)
      throws IOException, ParseException, SQLException {
    Map<String, Object> map = new HashMap<>();
    map.put(
        "users",
        getAllUsers(
            0,
            100,
            session.getId(),
            userGroupController.getNameMap().get("NotAdmin"),
            mainDataSource));
    return map;
  }

  /**
   * Gets the user group controller.
   *
   * @return the user group controller
   */
  UserGroupController getUserGroupController() {
    return userGroupController;
  }

  /**
   * Sets the user group controller.
   *
   * @param userGroupController the new user group controller
   */
  void setUserGroupController(UserGroupController userGroupController) {
    this.userGroupController = userGroupController;
  }
}
