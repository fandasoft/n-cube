package com.cedarsoftware.ncube;

import com.cedarsoftware.util.StringUtilities;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class binds together Account, App, and version.  These fields together
 * completely identify the application (and version) that a given n-cube belongs
 * to.
 *
 * @author John DeRegnaucourt (jdereg@gmail.com)
 *         <br/>
 *         Copyright (c) Cedar Software LLC
 *         <br/><br/>
 *         Licensed under the Apache License, Version 2.0 (the "License");
 *         you may not use this file except in compliance with the License.
 *         You may obtain a copy of the License at
 *         <br/><br/>
 *         http://www.apache.org/licenses/LICENSE-2.0
 *         <br/><br/>
 *         Unless required by applicable law or agreed to in writing, software
 *         distributed under the License is distributed on an "AS IS" BASIS,
 *         WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *         See the License for the specific language governing permissions and
 *         limitations under the License.
 */
public class ApplicationID
{
    public static final String DEFAULT_TENANT = "NONE";
    public static final String DEFAULT_APP = "DEFAULT_APP";
    public static final String DEFAULT_VERSION = "999.99.9";
    private static final Pattern versionPattern = Pattern.compile("^\\d+\\.\\d+\\.\\d+$");
    private final String account;
    private final String app;
    private final String version;
    private final String status;

    // For serialization support only
    private ApplicationID()
    {
        account = DEFAULT_TENANT;
        app = DEFAULT_APP;
        version = DEFAULT_VERSION;
        status = ReleaseStatus.SNAPSHOT.name();
    }

    public ApplicationID(String account, String app, String version, String status)
    {
        this.account = account;
        this.app = app;
        this.version = version;
        this.status = status;
        validate();
    }

    public String getAccount()
    {
        return account;
    }

    public String getApp()
    {
        return app;
    }

    public String getVersion()
    {
        return version;
    }

    public String getStatus()
    {
        return status;
    }

    public String cacheKey()
    {
        return cacheKey("");
    }

    public String cacheKey(String name)
    {
        StringBuilder s = new StringBuilder();
        s.append(account);
        s.append('/');
        s.append(app);
        s.append('/');
        s.append(version);
        s.append('/');
        s.append(name);
        return s.toString().toLowerCase();
    }

    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }

        if (!(o instanceof ApplicationID))
        {
            return false;
        }

        ApplicationID that = (ApplicationID) o;

        if (!account.equalsIgnoreCase(that.account))
        {
            return false;
        }
        if (!app.equalsIgnoreCase(that.app))
        {
            return false;
        }
        if (!status.equals(that.status))
        {
            return false;
        }
        if (!version.equals(that.version))
        {
            return false;
        }

        return true;
    }

    public int hashCode()
    {
        int result = account.toLowerCase().hashCode();
        result = 31 * result + app.toLowerCase().hashCode();
        result = 31 * result + version.hashCode();
        result = 31 * result + status.hashCode();
        return result;
    }

    public String toString()
    {
        return cacheKey();
    }

    public boolean isSnapshot()
    {
        return ReleaseStatus.SNAPSHOT.name().equals(status);
    }

    public boolean isRelease()
    {
        return ReleaseStatus.RELEASE.name().equals(status);
    }

    /**
     * Creates a new SNAPSHOT version of this application id.
     * @param version new version.
     * @return a new ApplicationId that is a snapshot of the new version passed in.
     */
    public ApplicationID createNewSnapshotId(String version)
    {
        //  In the Change Version the status was always SNAPSHOT when creating a new version.
        //  That is why we hardcode this to snapshot here.
        return new ApplicationID(account, app, version, ReleaseStatus.SNAPSHOT.name());
    }

    public void validate()
    {
        validateTenant(account);
        validateApp(app);
        validateVersion(version);
        validateStatus(status);
    }

    public static void validateTenant(String tenant)
    {
        if (StringUtilities.isEmpty(tenant))
        {
            throw new IllegalArgumentException("Tenant cannot be null or empty");
        }
    }

    public static void validateApp(String app)
    {
        if (StringUtilities.isEmpty(app))
        {
            throw new IllegalArgumentException("App cannot be null or empty");
        }
    }

    public static void validateStatus(String status)
    {
        if (status == null) {
            throw new IllegalArgumentException("status name cannot be null");
        }
        ReleaseStatus.valueOf(status);
    }

    public static void validateVersion(String version)
    {
        if (StringUtilities.isEmpty(version))
        {
            throw new IllegalArgumentException("n-cube version cannot be null or empty");
        }

        Matcher m = Regexes.validVersion.matcher(version);
        if (m.find())
        {
            return;
        }
        throw new IllegalArgumentException("Invalid version: '" + version + "'. n-cube version must follow the form n.n.n where n is a number 0 or greater. The numbers stand for major.minor.revision");
    }
}
