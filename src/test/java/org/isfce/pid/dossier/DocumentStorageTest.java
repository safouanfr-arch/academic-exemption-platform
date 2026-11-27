package org.isfce.pid.dossier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.isfce.pid.dao.IDocumentDao;
import org.isfce.pid.model.Document;
import org.isfce.pid.model.Dossier;
import org.isfce.pid.model.TypeDocument;
import org.isfce.pid.service.DossierService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class DocumentStorageTest {
	@TempDir
	Path root;

	@Mock
	IDocumentDao documentDao;

	@InjectMocks
	DossierService service;

	@BeforeEach
	void isolateFilesystem() {
		ReflectionTestUtils.setField(service, "racineUpload", root.toString());
	}

	@Test
	void deletesOnlyTheFileManagedForTheDocument() throws Exception {
		when(documentDao.saveAndFlush(any(Document.class))).thenAnswer(call -> call.getArgument(0));
		var document = service.stockerDocumentTeleverse(dossier(), sample(), TypeDocument.BULLETIN, null);
		var file = root.resolve("dossier-7").resolve(document.getCleStockage());
		assertThat(file).exists();
		assertThat(document.getCheminFichier()).isNull();

		when(documentDao.findById(25L)).thenReturn(Optional.of(document));
		service.supprimerDocument(25L);

		verify(documentDao).deleteById(25L);
		assertThat(file).doesNotExist();
	}

	@Test
	void deletingDocumentReferenceLeavesTheReferencedFileIntact() throws Exception {
		Path file = Files.writeString(root.resolve("unrelated.txt"), "synthetic evidence");
		var document = Document.builder().nomFichier("reference.txt").cheminFichier(file.toString()).build();
		when(documentDao.findById(26L)).thenReturn(Optional.of(document));

		service.supprimerDocument(26L);

		verify(documentDao).deleteById(26L);
		assertThat(file).exists();
	}

	@Test
	void removesUncommittedFileIfMetadataPersistenceFails() throws Exception {
		when(documentDao.saveAndFlush(any(Document.class))).thenThrow(new DataIntegrityViolationException("test"));

		assertThatThrownBy(() -> service.stockerDocumentTeleverse(dossier(), sample(), TypeDocument.BULLETIN, null))
				.isInstanceOf(DataIntegrityViolationException.class);
		assertThat(root.resolve("dossier-7")).isEmptyDirectory();
	}

	private Dossier dossier() {
		Dossier dossier = mock(Dossier.class);
		when(dossier.getId()).thenReturn(7L);
		return dossier;
	}

	private MockMultipartFile sample() {
		return new MockMultipartFile("file", "fixture.txt", "text/plain", new byte[] {1, 2, 3});
	}
}
