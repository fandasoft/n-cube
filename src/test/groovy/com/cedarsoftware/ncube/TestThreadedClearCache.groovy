package com.cedarsoftware.ncube

import groovy.transform.CompileStatic
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Test

import java.security.SecureRandom
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

import static org.junit.Assert.assertEquals

/**
 * @author John DeRegnaucourt (jdereg@gmail.com)
 *         <br/>
 *         Copyright (c) Cedar Software LLC
 *         <br/><br/>
 *         Licensed under the Apache License, Version 2.0 (the 'License')
 *         you may not use this file except in compliance with the License.
 *         You may obtain a copy of the License at
 *         <br/><br/>
 *         http://www.apache.org/licenses/LICENSE-2.0
 *         <br/><br/>
 *         Unless required by applicable law or agreed to in writing, software
 *         distributed under the License is distributed on an 'AS IS' BASIS,
 *         WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *         See the License for the specific language governing permissions and
 *         limitations under the License.
 */
@CompileStatic
class TestThreadedClearCache
{
    public static String USER_ID = TestNCubeManager.USER_ID
    public static ApplicationID appId = new ApplicationID(ApplicationID.DEFAULT_TENANT, "clearCacheTest", ApplicationID.DEFAULT_VERSION, ApplicationID.DEFAULT_STATUS, ApplicationID.TEST_BRANCH)
    public static ApplicationID usedId = new ApplicationID(ApplicationID.DEFAULT_TENANT, "usedInvalidId", ApplicationID.DEFAULT_VERSION, ApplicationID.DEFAULT_STATUS, ApplicationID.TEST_BRANCH)

    private TestingDatabaseManager manager;

    @Before
    void setup()
    {
        manager = TestingDatabaseHelper.testingDatabaseManager
        manager.setUp()

        NCubeManager.NCubePersister = TestingDatabaseHelper.persister
    }

    @After
    void tearDown()
    {
        manager.tearDown()
        manager = null

        NCubeManager.clearCache()
    }

    // Uncomment when testing threading.  This hits an external website, so use sparingly
    @Ignore
    void testCubesWithThreadedClearCacheWithAppId()
    {
        NCube[] ncubes = TestingDatabaseHelper.getCubesFromDisk("sys.classpath.2per.app.json", "math.controller.json")

        // add cubes for this test.
        manager.addCubes(usedId, USER_ID, ncubes)

        concurrencyTest()

        // remove cubes
        manager.removeBranches([usedId] as ApplicationID[])
    }

    private void concurrencyTest()
    {
        int numThreads = 8
        final CountDownLatch startLatch = new CountDownLatch(1)
        final CountDownLatch finishedLatch = new CountDownLatch(numThreads)
        final AtomicBoolean failed = new AtomicBoolean(false)
        ExecutorService executor = Executors.newFixedThreadPool(numThreads)
        Random random = new SecureRandom()

        for (int taskCount = 0; taskCount < numThreads; taskCount++)
        {
            executor.execute(new Runnable() {
                void run() {
                    try
                    {
                        startLatch.await()
                        NCube cube = NCubeManager.getCube(usedId, "MathController")

                        for (int i = 0; i < 1000; i++)
                        {
                            if (random.nextInt(100) == 42)
                            {   // 1/100th of the time, clear the cache
                                NCubeManager.clearCache()
                            }
                            else
                            {   // 99/100ths of the time, execute cells
                                def input = [:]
                                input.env = "a"
                                input.x = 5
                                input.method = 'square'

                                assertEquals(25, cube.getCell(input))

                                input.method = 'factorial'
                                assertEquals(120, cube.getCell(input))

                                input.env = "b"
                                input.x = 6
                                input.method = 'square'
                                assertEquals(6, cube.getCell(input))
                                input.method = 'factorial'
                                assertEquals(6, cube.getCell(input))
                            }
                        }
                    }
                    catch (Exception e)
                    {
                        Throwable t = getDeepestException(e)
                        if (!(t.message?.contains('cleared while cell was executing')))
                        {
                            failed.set(true)
                            throw e
                        }
                        else
                        {
                            println 'benign - code cleared while cell was executing'
                        }
                    }
                    finally
                    {
                        finishedLatch.countDown()
                    }
                }
            })
        }
        startLatch.countDown()  // trigger all Runnables to start
        finishedLatch.await()   // wait for all Runnables to finish
        executor.shutdown()
        assert !failed.get()
    }

    /**
     * Get the deepest (original cause) of the exception chain.
     * @param e Throwable exception that occurred.
     * @return Throwable original (causal) exception
     */
    static Throwable getDeepestException(Throwable e)
    {
        while (e.cause != null)
        {
            e = e.cause
        }

        return e
    }
}
