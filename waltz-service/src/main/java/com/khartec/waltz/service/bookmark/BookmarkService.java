/*
 * Waltz - Enterprise Architecture
 * Copyright (C) 2016, 2017 Waltz open source project
 * See README.md for more information
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.khartec.waltz.service.bookmark;

import com.khartec.waltz.data.GenericSelector;
import com.khartec.waltz.data.GenericSelectorFactory;
import com.khartec.waltz.data.bookmark.BookmarkDao;
import com.khartec.waltz.data.bookmark.BookmarkIdSelectorFactory;
import com.khartec.waltz.model.*;
import com.khartec.waltz.model.bookmark.Bookmark;
import com.khartec.waltz.model.changelog.ImmutableChangeLog;
import com.khartec.waltz.service.changelog.ChangeLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

import static com.khartec.waltz.common.Checks.checkNotNull;


@Service
public class BookmarkService {

    private final BookmarkDao bookmarkDao;
    private final ChangeLogService changeLogService;
    private final BookmarkIdSelectorFactory bookmarkIdSelectorFactory;
    private final GenericSelectorFactory genericSelectorFactory;


    @Autowired
    public BookmarkService(BookmarkDao bookmarkDao,
                           BookmarkIdSelectorFactory bookmarkIdSelectorFactory,
                           GenericSelectorFactory genericSelectorFactory,
                           ChangeLogService changeLogService) {
        checkNotNull(bookmarkDao, "bookmarkDao must not be null");
        checkNotNull(bookmarkIdSelectorFactory, "bookmarkIdSelectorFactory cannot be null");
        checkNotNull(genericSelectorFactory, "genericSelectorFactory cannot be null");
        checkNotNull(changeLogService, "changeLogService cannot be null");
        this.bookmarkDao = bookmarkDao;
        this.bookmarkIdSelectorFactory = bookmarkIdSelectorFactory;
        this.genericSelectorFactory = genericSelectorFactory;
        this.changeLogService = changeLogService;
    }


    public List<Bookmark> findByReference(EntityReference reference) {
        return bookmarkDao.findByReference(reference);
    }


    public Bookmark create(Bookmark bookmark, String username) {
        logChange("Added", bookmark, username, Operation.ADD);
        return bookmarkDao.create(bookmark, username);
    }


    public Bookmark update(Bookmark bookmark, String username) {
        logChange("Updated", bookmark, username, Operation.UPDATE);
        return bookmarkDao.update(bookmark, username);
    }


    /**
     * @param bookmark
     * @return true if bookmark deleted
     */
    public boolean deleteById(Bookmark bookmark, String username) {
        logChange("Removed", bookmark, username, Operation.REMOVE);
        return bookmarkDao.deleteById(bookmark.id().get());
    }


    public Bookmark getById(long bookmarkId) {
        return bookmarkDao.getById(bookmarkId);
    }


    private void logChange(String verb, Bookmark bookmark, String username, Operation operation) {
        changeLogService.write(ImmutableChangeLog.builder()
                .message(String.format("%s bookmark: %s / %s",
                        verb,
                        bookmark.title().orElse("?"),
                        bookmark.bookmarkKind()))
                .parentReference(bookmark.parent())
                .userId(username)
                .severity(Severity.INFORMATION)
                .childKind(EntityKind.BOOKMARK)
                .operation(operation)
                .build());
    }



    public Collection<Bookmark> findByBookmarkIdSelector(IdSelectionOptions selectionOptions) {
        return bookmarkDao.findByBookmarkIdSelector(bookmarkIdSelectorFactory.apply(selectionOptions));
    }

    public int deleteByBookmarkIdSelector(IdSelectionOptions selectionOptions) {
        GenericSelector selector = genericSelectorFactory.apply(selectionOptions);
        return bookmarkDao
                .deleteByParentSelector(selector);
    }

}
