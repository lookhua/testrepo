/*! ******************************************************************************
 *
 * Pentaho Data Integration
 *
 * Copyright (C) 2002-2016 by Pentaho : http://www.pentaho.com
 *
 *******************************************************************************
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 ******************************************************************************/

package org.pentaho.di.ui.repo;

import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.pentaho.di.core.KettleEnvironment;
import org.pentaho.di.core.database.DatabaseMeta;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.plugins.PluginInterface;
import org.pentaho.di.core.plugins.PluginRegistry;
import org.pentaho.di.core.plugins.RepositoryPluginType;
import org.pentaho.di.repository.AbstractRepository;
import org.pentaho.di.repository.RepositoriesMeta;
import org.pentaho.di.repository.Repository;
import org.pentaho.di.repository.RepositoryMeta;
import org.pentaho.di.repository.filerep.KettleFileRepositoryMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.*;

/**
 * Created by bmorrise on 5/3/16.
 */
@RunWith( MockitoJUnitRunner.class )
public class RepositoryConnectControllerTest {

  public static final String PLUGIN_NAME = "PLUGIN NAME";
  public static final String ID = "ID";
  public static final String PLUGIN_DESCRIPTION = "PLUGIN DESCRIPTION";
  public static final String DATABASE_NAME = "DATABASE NAME";
  public static final String REPOSITORY_NAME = "Repository Name";
  public static final String REPOSITORY_ID = "Repository ID";
  public static final String REPOSITORY_DESCRIPTION = "Repository Description";

  @Mock
  RepositoriesMeta repositoriesMeta;

  @Mock
  PluginRegistry pluginRegistry;

  @Mock
  RepositoryMeta repositoryMeta;

  @Mock
  PluginInterface pluginInterface;

  @Mock
  AbstractRepository repository;

  @Mock
  DatabaseMeta databaseMeta;

  private RepositoryConnectController controller;

  @BeforeClass
  public static void setUpClass() throws Exception {
    if ( !KettleEnvironment.isInitialized() ) {
      KettleEnvironment.init();
    }
  }

  @Before
  public void setUp() {
    controller = new RepositoryConnectController( pluginRegistry, null, repositoriesMeta );

    when( pluginInterface.getName() ).thenReturn( PLUGIN_NAME );
    when( pluginInterface.getIds() ).thenReturn( new String[] { ID } );
    when( pluginInterface.getDescription() ).thenReturn( PLUGIN_DESCRIPTION );

    List<PluginInterface> plugins = new ArrayList<>();
    plugins.add( pluginInterface );

    when( pluginRegistry.getPlugins( RepositoryPluginType.class ) ).thenReturn( plugins );

    when( repositoryMeta.getId() ).thenReturn( ID );
    when( repositoryMeta.getName() ).thenReturn( PLUGIN_NAME );
    when( repositoryMeta.getDescription() ).thenReturn( PLUGIN_DESCRIPTION );
  }

  @Test
  public void testGetPlugins() throws Exception {
    String plugins = controller.getPlugins();
    assertEquals( "[{\"name\":\"PLUGIN NAME\",\"description\":\"PLUGIN DESCRIPTION\",\"id\":\"ID\"}]", plugins );
  }

  @Test
  public void testCreateRepository() throws Exception {
    String id = ID;
    Map<String, Object> items = new HashMap<>();

    when( pluginRegistry.loadClass( RepositoryPluginType.class, id, RepositoryMeta.class ) )
      .thenReturn( repositoryMeta );
    when( pluginRegistry.loadClass( RepositoryPluginType.class, repositoryMeta.getId(), Repository.class ) )
      .thenReturn( repository );

    when( repository.test() ).thenReturn( true );

    boolean result = controller.createRepository( id, items );

    assertEquals( true, result );

    when( repository.test() ).thenReturn( false );

    result = controller.createRepository( id, items );

    assertEquals( false, result );

    when( repository.test() ).thenReturn( true );
    doThrow( new KettleException() ).when( repositoriesMeta ).writeData();

    result = controller.createRepository( id, items );
    assertEquals( false, result );
  }

  @Test
  public void testGetRepositories() {
    when( repositoriesMeta.nrRepositories() ).thenReturn( 1 );
    when( repositoriesMeta.getRepository( 0 ) ).thenReturn( repositoryMeta );

    JSONObject json = new JSONObject();
    json.put( "displayName", REPOSITORY_NAME );
    json.put( "isDefault", false );
    json.put( "description", REPOSITORY_DESCRIPTION );
    json.put( "id", REPOSITORY_ID );

    when( repositoryMeta.toJSONObject() ).thenReturn( json );

    String repositories = controller.getRepositories();

    assertEquals(
      "[{\"isDefault\":false,\"displayName\":\"Repository Name\",\"description\":\"Repository Description\","
        + "\"id\":\"Repository ID\"}]",
      repositories );
  }

  @Test
  public void testConnectToRepository() throws Exception {
    when( pluginRegistry.loadClass( RepositoryPluginType.class, repositoryMeta.getId(), Repository.class ) )
      .thenReturn( repository );

    controller.setCurrentRepository( repositoryMeta );
    controller.connectToRepository();

    verify( repository ).init( repositoryMeta );
    verify( repository ).connect( null, null );
  }

  @Test
  public void testGetDatabases() throws Exception {
    when( repositoriesMeta.nrDatabases() ).thenReturn( 1 );
    when( repositoriesMeta.getDatabase( 0 ) ).thenReturn( databaseMeta );
    when( databaseMeta.getName() ).thenReturn( DATABASE_NAME );

    String databases = controller.getDatabases();
    assertEquals( "[{\"name\":\"DATABASE NAME\"}]", databases );
  }

  @Test
  public void testDeleteRepository() throws Exception {
    int index = 1;
    when( repositoriesMeta.findRepository( REPOSITORY_NAME ) ).thenReturn( repositoryMeta );
    when( repositoriesMeta.indexOfRepository( repositoryMeta ) ).thenReturn( index );
    when( repositoriesMeta.getRepository( index ) ).thenReturn( repositoryMeta );

    boolean result = controller.deleteRepository( REPOSITORY_NAME );

    assertEquals( true, result );
    verify( repositoriesMeta ).removeRepository( index );
    verify( repositoriesMeta ).writeData();
  }

  @Test
  public void testSetDefaultRepository() {
    int index = 1;
    when( repositoriesMeta.findRepository( REPOSITORY_NAME ) ).thenReturn( repositoryMeta );
    when( repositoriesMeta.indexOfRepository( repositoryMeta ) ).thenReturn( index );

    boolean result = controller.setDefaultRepository( REPOSITORY_NAME );
    assertEquals( true, result );
  }

  @Test
  public void testAddDatabase() throws Exception {
    controller.addDatabase( databaseMeta );

    verify( repositoriesMeta ).addDatabase( databaseMeta );
    verify( repositoriesMeta ).writeData();
  }

  @Test
  public void testGetDefaultUrl() throws Exception {
    String defaultUrl = controller.getDefaultUrl();
    assertNotNull( defaultUrl );
  }

  @Test
  public void testGetRepository() throws Exception {
    KettleFileRepositoryMeta kettleFileRepositoryMeta = new KettleFileRepositoryMeta();
    kettleFileRepositoryMeta.setId( REPOSITORY_ID );
    kettleFileRepositoryMeta.setDescription( REPOSITORY_DESCRIPTION );
    kettleFileRepositoryMeta.setName( REPOSITORY_NAME );

    when( repositoriesMeta.findRepository( REPOSITORY_NAME ) ).thenReturn( kettleFileRepositoryMeta );

    String output = controller.getRepository( REPOSITORY_NAME );

    assertEquals( true, output.contains( REPOSITORY_ID ) );
    assertEquals( true, output.contains( REPOSITORY_DESCRIPTION ) );
    assertEquals( true, output.contains( REPOSITORY_NAME ) );
  }
}
