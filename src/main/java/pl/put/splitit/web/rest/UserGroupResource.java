package pl.put.splitit.web.rest;

import com.codahale.metrics.annotation.Timed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import pl.put.splitit.domain.Transaction;
import pl.put.splitit.domain.User;
import pl.put.splitit.domain.UserGroup;
import pl.put.splitit.service.TransactionService;
import pl.put.splitit.service.UserGroupService;
import pl.put.splitit.service.UserService;
import pl.put.splitit.web.rest.util.HeaderUtil;
import pl.put.splitit.web.rest.util.PaginationUtil;

import javax.inject.Inject;
import javax.validation.Valid;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * REST controller for managing UserGroup.
 */
@RestController
@RequestMapping("/api")
public class UserGroupResource {

    private final Logger log = LoggerFactory.getLogger(UserGroupResource.class);

    @Inject
    private UserGroupService userGroupService;

    @Inject
    private UserService userService;

    @Inject
    private TransactionService transactionService;

    /**
     * POST  /groups : Create a new userGroup.
     *
     * @param userGroup the userGroup to create
     * @return the ResponseEntity with status 201 (Created) and with body the new userGroup, or with status 400 (Bad Request) if the userGroup has already an ID
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PostMapping("/groups")
    @Timed
    public ResponseEntity<UserGroup> createUserGroup(@Valid @RequestBody UserGroup userGroup) throws URISyntaxException {
        log.debug("REST request to save UserGroup : {}", userGroup);
        if (userGroup.getId() != null) {
            return ResponseEntity.badRequest().headers(HeaderUtil.createFailureAlert("userGroup", "idexists", "A new userGroup cannot already have an ID")).body(null);
        }

        User owner = userService.getUserWithAuthorities();
        userGroup.setOwner(owner);
        userGroup.getUsers().add(owner);


        UserGroup result = userGroupService.save(userGroup);
        return ResponseEntity.created(new URI("/api/groups/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert("userGroup", result.getId().toString()))
            .body(result);
    }

    /**
     * PUT  /groups : Updates an existing userGroup.
     *
     * @param userGroup the userGroup to update
     * @return the ResponseEntity with status 200 (OK) and with body the updated userGroup,
     * or with status 400 (Bad Request) if the userGroup is not valid,
     * or with status 500 (Internal Server Error) if the userGroup couldnt be updated
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PutMapping("/groups")
    @Timed
    public ResponseEntity<UserGroup> updateUserGroup(@Valid @RequestBody UserGroup userGroup) throws URISyntaxException {
        log.debug("REST request to update UserGroup : {}", userGroup);
        if (userGroup.getId() == null) {
            return createUserGroup(userGroup);
        }
        UserGroup result = userGroupService.save(userGroup);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert("userGroup", userGroup.getId().toString()))
            .body(result);
    }

    /**
     * GET  /groups : get all the userGroups.
     *
     * @param pageable the pagination information
     * @return the ResponseEntity with status 200 (OK) and the list of userGroups in body
     * @throws URISyntaxException if there is an error to generate the pagination HTTP headers
     */
    @GetMapping("/groups")
    @Timed
    public ResponseEntity<List<UserGroup>> getAllUserGroups(Pageable pageable)
        throws URISyntaxException {
        log.debug("REST request to get a page of UserGroups");
        Page<UserGroup> page = userGroupService.findAll(pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(page, "/api/groups");
        return new ResponseEntity<>(page.getContent(), headers, HttpStatus.OK);
    }

    @GetMapping("/groups/private")
    @Timed
    public ResponseEntity<List<UserGroup>> getAllUserPrivateGroups(Pageable pageable)
        throws URISyntaxException {
        log.debug("REST request to get a page of UserGroups");
        Page<UserGroup> page = userGroupService.findAllPublic(pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(page, "/api/groups/private");
        return new ResponseEntity<>(page.getContent(), headers, HttpStatus.OK);
    }

    /**
     * GET  /groups/:id : get the "id" userGroup.
     *
     * @param id the id of the userGroup to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the userGroup, or with status 404 (Not Found)
     */
    @GetMapping("/groups/{id}")
    @Timed
    public ResponseEntity<UserGroup> getUserGroup(@PathVariable Long id) {
        log.debug("REST request to get UserGroup : {}", id);
        UserGroup userGroup = userGroupService.findOne(id);
        return Optional.ofNullable(userGroup)
            .map(result -> new ResponseEntity<>(
                result,
                HttpStatus.OK))
            .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }


    @GetMapping("/groups/{id}/transactions")
    @Timed
    public ResponseEntity<Page<Transaction>> getUserGroupTransactions(@PathVariable Long id, Pageable pageable) throws URISyntaxException {
        log.debug("REST request to get transactions of group: {}", id);
        UserGroup userGroup = userGroupService.findOne(id);
        if (userGroup == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        Page<Transaction> transactions = transactionService.findAllByGroup(id, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(transactions, String.format("/api/users/%s/transactions", id));
        return new ResponseEntity<>(transactions, headers, HttpStatus.OK);
    }

    @GetMapping("/groups/{id}/users")
    @Timed
    public ResponseEntity<Collection<User>> getUserGroupMembers(@PathVariable Long id) throws URISyntaxException {
        log.debug("REST request to get members of group: {}", id);
        UserGroup userGroup = userGroupService.findOne(id);
        if (userGroup == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        // Get all members besides owner
        Set<User> members = userGroup.getUsers();
        // add owner who always is a member of its own group
        members.add(userGroup.getOwner());

        return new ResponseEntity<>(members, HttpStatus.OK);
    }


    /**
     * DELETE  /groups/:id : delete the "id" userGroup.
     *
     * @param id the id of the userGroup to delete
     * @return the ResponseEntity with status 200 (OK)
     */
    @DeleteMapping("/groups/{id}")
    @Timed
    public ResponseEntity<Void> deleteUserGroup(@PathVariable Long id) {
        log.debug("REST request to delete UserGroup : {}", id);
        userGroupService.delete(id);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityDeletionAlert("userGroup", id.toString())).build();
    }

}
