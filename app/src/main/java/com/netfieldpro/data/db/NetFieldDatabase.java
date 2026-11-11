package com.netfieldpro.data.db;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.netfieldpro.data.db.dao.NetFieldDao;
import com.netfieldpro.data.db.entity.AccessPointMarkerRow;
import com.netfieldpro.data.db.entity.ApGroupRow;
import com.netfieldpro.data.db.entity.AssessmentRow;
import com.netfieldpro.data.db.entity.ConnectionEventRow;
import com.netfieldpro.data.db.entity.DeviceInfoRow;
import com.netfieldpro.data.db.entity.FloorBoundaryRow;
import com.netfieldpro.data.db.entity.FloorRow;
import com.netfieldpro.data.db.entity.LatencyResultRow;
import com.netfieldpro.data.db.entity.LinkSampleRow;
import com.netfieldpro.data.db.entity.MeasurementRow;
import com.netfieldpro.data.db.entity.PathSegmentRow;
import com.netfieldpro.data.db.entity.ProjectRow;
import com.netfieldpro.data.db.entity.RawSampleRow;
import com.netfieldpro.data.db.entity.RequirementProfileRow;
import com.netfieldpro.data.db.entity.ScanRow;
import com.netfieldpro.data.db.entity.SurveyPointRow;
import com.netfieldpro.data.db.entity.SurveySessionRow;
import com.netfieldpro.data.db.entity.TargetNetworkRow;
import com.netfieldpro.data.db.entity.WallMaterialRow;
import com.netfieldpro.data.db.entity.WallRow;

@Database(
        entities = {
                ProjectRow.class,
                FloorRow.class,
                SurveySessionRow.class,
                DeviceInfoRow.class,
                ScanRow.class,
                RawSampleRow.class,
                SurveyPointRow.class,
                MeasurementRow.class,
                LatencyResultRow.class,
                LinkSampleRow.class,
                ConnectionEventRow.class,
                PathSegmentRow.class,
                AccessPointMarkerRow.class,
                TargetNetworkRow.class,
                ApGroupRow.class,
                FloorBoundaryRow.class,
                WallRow.class,
                WallMaterialRow.class,
                RequirementProfileRow.class,
                AssessmentRow.class
        },
        version = 1,
        exportSchema = true
)
public abstract class NetFieldDatabase extends RoomDatabase {

    private static volatile NetFieldDatabase INSTANCE;
    private static volatile Context lastAppContext;

    public abstract NetFieldDao netFieldDao();

    public static final Callback SEED_CALLBACK = new Callback() {
        @Override
        public void onCreate(@NonNull SupportSQLiteDatabase db) {
            super.onCreate(db);
            db.execSQL("INSERT OR REPLACE INTO wall_material (material_id, penalty_m, loss_db24, loss_db5) VALUES ('DRYWALL_LIGHT', 3.0, 3.0, 4.5)");
            db.execSQL("INSERT OR REPLACE INTO wall_material (material_id, penalty_m, loss_db24, loss_db5) VALUES ('GLASS_PARTITION', 2.5, 2.5, 4.0)");
            db.execSQL("INSERT OR REPLACE INTO wall_material (material_id, penalty_m, loss_db24, loss_db5) VALUES ('BRICK_MEDIUM', 8.0, 8.0, 12.0)");
            db.execSQL("INSERT OR REPLACE INTO wall_material (material_id, penalty_m, loss_db24, loss_db5) VALUES ('CONCRETE_HEAVY', 15.0, 12.0, 18.0)");
            db.execSQL("INSERT OR REPLACE INTO wall_material (material_id, penalty_m, loss_db24, loss_db5) VALUES ('METAL_ELEVATOR', 25.0, 20.0, 28.0)");
        }

        @Override
        public void onOpen(@NonNull SupportSQLiteDatabase db) {
            super.onOpen(db);
            db.execSQL("PRAGMA foreign_keys = ON;");
        }
    };

    public static void resetInstance() {
        synchronized (NetFieldDatabase.class) {
            if (INSTANCE != null && INSTANCE.isOpen()) {
                try {
                    INSTANCE.close();
                } catch (Exception ignored) {
                }
            }
            INSTANCE = null;
            lastAppContext = null;
        }
    }

    public static NetFieldDatabase get(Context context) {
        Context appContext = context.getApplicationContext();
        if (INSTANCE == null || lastAppContext != appContext || !INSTANCE.isOpen()) {
            synchronized (NetFieldDatabase.class) {
                if (INSTANCE == null || lastAppContext != appContext || !INSTANCE.isOpen()) {
                    if (INSTANCE != null) {
                        try {
                            INSTANCE.close();
                        } catch (Exception ignored) {
                        }
                    }
                    lastAppContext = appContext;
                    INSTANCE = Room.databaseBuilder(
                                    appContext,
                                    NetFieldDatabase.class,
                                    "netfield.db"
                            )
                            .addCallback(SEED_CALLBACK)
                            .allowMainThreadQueries()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
