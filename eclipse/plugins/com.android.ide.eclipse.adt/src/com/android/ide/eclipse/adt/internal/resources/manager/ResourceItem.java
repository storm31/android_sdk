/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Eclipse Public License, Version 1.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.eclipse.org/org/documents/epl-v10.php
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.ide.eclipse.adt.internal.resources.manager;

import com.android.ide.eclipse.adt.internal.resources.configurations.FolderConfiguration;
import com.android.resources.ResourceType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * An android resource.
 *
 * This is a representation of the resource, not of its value(s). It gives access to all
 * the source files that generate this particular resource which then can be used to access
 * the actual value(s).
 *
 * @see ResourceFile#getResources(ResourceType, ResourceRepository)
 */
public class ResourceItem implements Comparable<ResourceItem> {

    private final static Comparator<ResourceFile> sComparator = new Comparator<ResourceFile>() {
        public int compare(ResourceFile file1, ResourceFile file2) {
            // get both FolderConfiguration and compare them
            FolderConfiguration fc1 = file1.getFolder().getConfiguration();
            FolderConfiguration fc2 = file2.getFolder().getConfiguration();

            return fc1.compareTo(fc2);
        }
    };

    private final String mName;

    /**
     * List of files generating this ResourceItem.
     */
    private final List<ResourceFile> mFiles = new ArrayList<ResourceFile>();

    /**
     * Constructs a new ResourceItem.
     * @param name the name of the resource as it appears in the XML and R.java files.
     */
    public ResourceItem(String name) {
        mName = name;
    }

    /**
     * Returns the name of the resource.
     */
    public final String getName() {
        return mName;
    }

    /**
     * Compares the {@link ResourceItem} to another.
     * @param other the ResourceItem to be compared to.
     */
    public int compareTo(ResourceItem other) {
        return mName.compareTo(other.mName);
    }

    /**
     * Returns whether the resource is editable directly.
     * <p/>
     * This is typically the case for resources that don't have alternate versions, or resources
     * of type {@link ResourceType#ID} that aren't declared inline.
     */
    public boolean isEditableDirectly() {
        return hasAlternates() == false;
    }

    /**
     * Returns whether the ID resource has been declared inline inside another resource XML file.
     * If the resource type is not {@link ResourceType#ID}, this will always return {@code false}.
     */
    public boolean isDeclaredInline() {
        return false;
    }

    /**
     * Adds a new source file.
     * @param file the source file.
     */
    protected void add(ResourceFile file) {
        mFiles.add(file);
    }

    /**
     * Removes a file from the list of source files.
     * @param file the file to remove
     */
    protected void removeFile(ResourceFile file) {
        mFiles.remove(file);
    }

    /**
     * Returns {@code true} if the item has no source file.
     * @return
     */
    protected boolean hasNoSourceFile() {
        return mFiles.size() == 0;
    }

    /**
     * Reset the item by emptying its source file list.
     */
    protected void reset() {
        mFiles.clear();
    }

    /**
     * Returns the sorted list of {@link ResourceItem} objects for this resource item.
     */
    public ResourceFile[] getSourceFileArray() {
        ArrayList<ResourceFile> list = new ArrayList<ResourceFile>();
        list.addAll(mFiles);

        Collections.sort(list, sComparator);

        return list.toArray(new ResourceFile[list.size()]);
    }

    /**
     * Returns the list of source file for this resource.
     */
    public List<ResourceFile> getSourceFileList() {
        return Collections.unmodifiableList(mFiles);
    }

    /**
     * Returns if the resource has at least one non-default version.
     *
     * @see ResourceFile#getConfiguration()
     * @see FolderConfiguration#isDefault()
     */
    public boolean hasAlternates() {
        for (ResourceFile file : mFiles) {
            if (file.getFolder().getConfiguration().isDefault() == false) {
                return true;
            }
        }

        return false;
    }

    /**
     * Returns whether the resource has a default version, with no qualifier.
     *
     * @see ResourceFile#getConfiguration()
     * @see FolderConfiguration#isDefault()
     */
    public boolean hasDefault() {
        for (ResourceFile file : mFiles) {
            if (file.getFolder().getConfiguration().isDefault()) {
                return true;
            }
        }

        // We only want to return false if there's no default and more than 0 items.
        return (mFiles.size() == 0);
    }

    /**
     * Returns the number of alternate versions for this resource.
     *
     * @see ResourceFile#getConfiguration()
     * @see FolderConfiguration#isDefault()
     */
    public int getAlternateCount() {
        int count = 0;
        for (ResourceFile file : mFiles) {
            if (file.getFolder().getConfiguration().isDefault() == false) {
                count++;
            }
        }

        return count;
    }

    @Override
    public String toString() {
        return "ResourceItem [mName=" + mName + ", mFiles=" + mFiles + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    }
}