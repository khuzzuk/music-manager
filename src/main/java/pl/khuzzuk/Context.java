package pl.khuzzuk;

import pl.khuzzuk.index.IndexReaderService;
import pl.khuzzuk.index.IndexService;
import pl.khuzzuk.metadata.MetadataIndexReaderService;
import pl.khuzzuk.metadata.MetadataIndexWriterService;
import pl.khuzzuk.metadata.MetadataReaderService;
import pl.khuzzuk.metadata.MetadataWriterService;
import pl.khuzzuk.playlist.SavedPlaylistService;
import pl.khuzzuk.player.SoundPlayer;
import pl.khuzzuk.settings.SettingsService;

public record Context(
        SettingsService settingsService,
        MetadataReaderService metadataReaderService,
        MetadataWriterService metadataWriterService,
        MetadataIndexReaderService metadataIndexReaderService,
        MetadataIndexWriterService metadataIndexWriterService,
        IndexService indexService,
        IndexReaderService indexReaderService,
        SavedPlaylistService savedPlaylistService,
        SoundPlayer soundPlayer) {
}
