package eu.kanade.tachiyomi.data.database.queries

import eu.kanade.tachiyomi.data.database.TriggerGenerator
import eu.kanade.tachiyomi.data.database.models.Updatable
import eu.kanade.tachiyomi.data.database.tables.MangaCategoryTable
import eu.kanade.tachiyomi.data.database.tables.SyncUpdatesTable

/**
 * Take a snapshot of the manga category table
 */
//language=sql
fun cloneMangaCategoriesQuery(id: String) = """
    CREATE TABLE IF NOT EXISTS ${MangaCategoryTable.SNAPSHOT_TABLE_PREFIX}$id AS SELECT * FROM ${MangaCategoryTable.TABLE}
        """

/**
 * Take an empty snapshot of the manga category table
 */
//language=sql
fun createEmptyClonedMangaCategoriesQuery(id: String) = """
    CREATE TABLE IF NOT EXISTS ${MangaCategoryTable.SNAPSHOT_TABLE_PREFIX}$id AS SELECT * FROM ${MangaCategoryTable.TABLE} WHERE 0
    """

/**
 * Delete the snapshot of the manga category table
 */
//language=sql
fun deleteClonedMangaCategoriesQuery(id: String) = """
    DROP TABLE IF EXISTS ${MangaCategoryTable.SNAPSHOT_TABLE_PREFIX}$id
        """

/**
 * Finds manga categories that exist in table `from` and do not exist in table `to`
 */
//language=sql
private fun genDiffMangaCategoriesQuery(from: String,
                                        to: String) = """
    SELECT * FROM $from WHERE NOT EXISTS
   (SELECT * FROM $to WHERE $from.${MangaCategoryTable.COL_MANGA_ID} = $to.${MangaCategoryTable.COL_MANGA_ID}
       AND $from.${MangaCategoryTable.COL_CATEGORY_ID} = $to.${MangaCategoryTable.COL_CATEGORY_ID})
    """

/**
 * Find manga categories that have been deleted since the last snapshot
 */
//Deleted categories exist in last snapshot and are missing from current table
fun getDeletedMangaCategoriesQuery(id: String) = genDiffMangaCategoriesQuery(
        MangaCategoryTable.SNAPSHOT_TABLE_PREFIX + id,
        MangaCategoryTable.TABLE
)

/**
 * Find manga categories that have been added since the last snapshot
 */
//Deleted categories exist in current table and are missing from last snapshot
fun getAddedMangaCategoriesQuery(id: String) = genDiffMangaCategoriesQuery(
        MangaCategoryTable.TABLE,
        MangaCategoryTable.SNAPSHOT_TABLE_PREFIX + id
)

//language=sql
val countMangaCategoriesQuery = """
    SELECT COUNT(1) FROM ${MangaCategoryTable.TABLE}
        WHERE ${MangaCategoryTable.COL_MANGA_ID} = ?
        AND ${MangaCategoryTable.COL_CATEGORY_ID} = ?
    """

//language=sql
val updateEntryUpdateQuery = """
    UPDATE ${SyncUpdatesTable.TABLE}
        SET ${SyncUpdatesTable.COL_DATETIME} = ?
        WHERE ${SyncUpdatesTable.COL_UPDATED_ROW} = ?
            AND ${SyncUpdatesTable.COL_FIELD} = ?
    """

fun getGenerateAllEntryUpdatesQueries(updatable: Updatable) = updatable.fields.map {
    //language=sql
    """
        INSERT INTO ${SyncUpdatesTable.TABLE} (${SyncUpdatesTable.COL_FIELD},
            ${SyncUpdatesTable.COL_DATETIME},
            ${SyncUpdatesTable.COL_UPDATED_ROW})
        SELECT ${it.id}, ${TriggerGenerator.CURRENT_TIME_SQL}, ${updatable.idColumn}
            FROM ${updatable.tableName} WHERE ${it.field} != ${TriggerGenerator.valueAsSQLiteLiteral(it.defValue)}
        """
}
