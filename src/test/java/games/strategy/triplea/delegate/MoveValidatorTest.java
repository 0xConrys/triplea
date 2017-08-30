package games.strategy.triplea.delegate;

import static games.strategy.triplea.delegate.GameDataTestUtil.addTo;
import static games.strategy.triplea.delegate.GameDataTestUtil.territory;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.TripleAUnit;
import games.strategy.triplea.delegate.dataObjects.MoveValidationResult;
import games.strategy.triplea.xml.TestMapGameData;
import games.strategy.util.Match;

public class MoveValidatorTest extends DelegateTest {

  private GameData twwGameData;

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();
    twwGameData = TestMapGameData.TWW.getGameData();
  }

  @Test
  public void testEnemyUnitsInPath() {
    // japanese unit in congo
    final Route bad = new Route();
    // the empty case
    assertTrue(MoveValidator.noEnemyUnitsOnPathMiddleSteps(bad, british, gameData));
    bad.add(egypt);
    bad.add(congo);
    bad.add(kenya);
    assertTrue(!MoveValidator.noEnemyUnitsOnPathMiddleSteps(bad, british, gameData));
    final Route good = new Route();
    good.add(egypt);
    good.add(kenya);
    assertTrue(MoveValidator.noEnemyUnitsOnPathMiddleSteps(good, british, gameData));
    // at end so should still be good
    good.add(congo);
    assertTrue(MoveValidator.noEnemyUnitsOnPathMiddleSteps(good, british, gameData));
  }

  @Test
  public void testHasUnitsThatCantGoOnWater() {
    final Collection<Unit> units = new ArrayList<>();
    units.addAll(infantry.create(1, british));
    units.addAll(armour.create(1, british));
    units.addAll(transport.create(1, british));
    units.addAll(fighter.create(1, british));
    assertTrue(!MoveValidator.hasUnitsThatCantGoOnWater(units));
    assertTrue(MoveValidator.hasUnitsThatCantGoOnWater(factory.create(1, british)));
  }

  @Test
  public void testCarrierCapacity() {
    final Collection<Unit> units = carrier.create(5, british);
    assertEquals(10, AirMovementValidator.carrierCapacity(units, new Territory("TestTerritory", true, gameData)));
  }

  @Test
  public void testCarrierCost() {
    final Collection<Unit> units = fighter.create(5, british);
    assertEquals(5, AirMovementValidator.carrierCost(units));
  }

  @Test
  public void testGetLeastMovement() {
    final Collection<Unit> collection = bomber.create(1, british);
    assertEquals(MoveValidator.getLeastMovement(collection), 6);
    final Object[] objs = collection.toArray();
    ((TripleAUnit) objs[0]).setAlreadyMoved(1);
    assertEquals(MoveValidator.getLeastMovement(collection), 5);
    collection.addAll(factory.create(2, british));
    assertEquals(MoveValidator.getLeastMovement(collection), 0);
  }

  @Test
  public void testCanLand() {
    final Collection<Unit> units = fighter.create(4, british);
    // 2 carriers in red sea
    assertTrue(AirMovementValidator.canLand(units, redSea, british, gameData));
    // britian owns egypt
    assertTrue(AirMovementValidator.canLand(units, egypt, british, gameData));
    // only 2 carriers
    final Collection<Unit> tooMany = fighter.create(6, british);
    assertTrue(!AirMovementValidator.canLand(tooMany, redSea, british, gameData));
    // nowhere to land
    assertTrue(!AirMovementValidator.canLand(units, japanSeaZone, british, gameData));
    // nuetral
    assertTrue(!AirMovementValidator.canLand(units, westAfrica, british, gameData));
  }

  @Test
  public void testCanLandInfantry() {
    try {
      final Collection<Unit> units = infantry.create(1, british);
      AirMovementValidator.canLand(units, redSea, british, gameData);
    } catch (final IllegalArgumentException e) {
      return;
    }
    fail("No exception thrown");
  }

  @Test
  public void testCanLandBomber() {
    final Collection<Unit> units = bomber.create(1, british);
    assertTrue(!AirMovementValidator.canLand(units, redSea, british, gameData));
  }

  @Test
  public void testHasSomeLand() {
    final Collection<Unit> units = transport.create(3, british);
    assertTrue(!Match.anyMatch(units, Matches.UnitIsLand));
    units.addAll(infantry.create(2, british));
    assertTrue(Match.anyMatch(units, Matches.UnitIsLand));
  }

  @Test
  public void testValidateMoveForRequiresUnitsToMove() {

    // Move regular units
    final PlayerID germans = GameDataTestUtil.germany(twwGameData);
    final Territory berlin = territory("Berlin", twwGameData);
    final Territory easternGermany = territory("Eastern Germany", twwGameData);
    final Route r = new Route(berlin, easternGermany);
    List<Unit> toMove = berlin.getUnits().getMatches(Matches.unitCanMove());
    MoveValidationResult results = MoveValidator.validateMove(toMove, r, germans, Collections.emptyList(),
        new HashMap<>(), false, null, twwGameData);
    assertTrue(results.isMoveValid());

    // Add germanTrain to units which fails since it requires germainRail
    addTo(berlin, GameDataTestUtil.germanTrain(twwGameData).create(1, germans));
    toMove = berlin.getUnits().getMatches(Matches.unitCanMove());
    results = MoveValidator.validateMove(toMove, r, germans, Collections.emptyList(),
        new HashMap<>(), false, null, twwGameData);
    assertFalse(results.isMoveValid());

    // Add germanRail to destination so move succeeds
    addTo(easternGermany, GameDataTestUtil.germanRail(twwGameData).create(1, germans));
    results = MoveValidator.validateMove(toMove, r, germans, Collections.emptyList(),
        new HashMap<>(), false, null, twwGameData);
    assertTrue(results.isMoveValid());
  }

}
