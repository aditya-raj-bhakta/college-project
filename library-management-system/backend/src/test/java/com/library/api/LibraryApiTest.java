package com.library.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Starts the whole app on an in-memory database (migrated by Flyway, filled by the demo seeder)
 * and drives it through the real REST API. Tests share the seeded library, so each one works
 * on titles and members the others leave alone.
 */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:lms;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
class LibraryApiTest {

    private static final String ADMIN = "admin@library.com:admin123";
    private static final String LIBRARIAN = "librarian@library.com:librarian123";
    private static final String RAHUL = "student@library.com:student123";
    private static final String PRIYA = "priya@library.com:priya123";
    private static final String ANITA = "faculty@library.com:faculty123";
    private static final String ANANYA = "ananya@library.com:ananya123";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    // ------------------------------------------------------------------ access

    @Test
    void everythingNeedsALogin() throws Exception {
        mvc.perform(get("/api/books")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void registrationGivesACardAndTheDefaultMemberType() throws Exception {
        register("New Student")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.role").value("MEMBER"))
                .andExpect(jsonPath("$.user.cardNumber").value(startsWith("M")))
                .andExpect(jsonPath("$.user.memberType.code").value("STUDENT"));
    }

    @Test
    void membersCannotUseStaffEndpoints() throws Exception {
        String rahul = login(RAHUL);
        mvc.perform(auth(post("/api/desk/checkin"), rahul).contentType(MediaType.APPLICATION_JSON)
                .content("{\"barcode\":\"SW000001\"}")).andExpect(status().isForbidden());
        mvc.perform(auth(get("/api/loans"), rahul)).andExpect(status().isForbidden());
        mvc.perform(auth(post("/api/books"), rahul).contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"T\",\"author\":\"A\",\"isbn\":\"1234567890\"}")).andExpect(status().isForbidden());
    }

    @Test
    void librariansCannotChangeRulesOrSeeTheAuditLog() throws Exception {
        String librarian = login(LIBRARIAN);
        mvc.perform(auth(get("/api/admin/audit"), librarian)).andExpect(status().isForbidden());
        mvc.perform(auth(put("/api/admin/settings"), librarian).contentType(MediaType.APPLICATION_JSON)
                .content("{\"holdPickupDays\":3,\"lostItemFee\":500,\"reminderDaysBefore\":2,\"defaultMemberTypeId\":1}"))
                .andExpect(status().isForbidden());
        mvc.perform(auth(get("/api/admin/audit"), login(ADMIN))).andExpect(status().isOk());
    }

    // ------------------------------------------------------------------ circulation

    @Test
    void memberBorrowsOnlineAndOnlyALibrarianChecksItIn() throws Exception {
        String member = token(register("Borrower " + UUID.randomUUID().toString().substring(0, 6)));
        long bookId = bookId("Operating System Concepts");

        JsonNode loan = body(mvc.perform(auth(post("/api/me/loans"), member).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bookId\":" + bookId + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.channel").value("SELF_SERVICE"))
                .andExpect(jsonPath("$.status").value("ACTIVE")));
        String barcode = loan.get("barcode").asText();

        // The member can't check it in themselves
        mvc.perform(auth(post("/api/desk/checkin"), member).contentType(MediaType.APPLICATION_JSON)
                .content("{\"barcode\":\"" + barcode + "\"}")).andExpect(status().isForbidden());

        mvc.perform(auth(post("/api/desk/checkin"), login(LIBRARIAN)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"barcode\":\"" + barcode + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.loan.status").value("RETURNED"))
                .andExpect(jsonPath("$.fineCharged").value(0));
    }

    @Test
    void librarianIssuesAtTheDeskByCardAndBarcode() throws Exception {
        String librarian = login(LIBRARIAN);
        JsonNode me = body(mvc.perform(auth(get("/api/me"), token(register("Desk Patron")))));
        String barcode = firstCopy(librarian, bookId("Computer Networking: A Top-Down Approach"), "AVAILABLE");

        mvc.perform(auth(post("/api/desk/checkout"), librarian).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cardNumber\":\"" + me.get("cardNumber").asText() + "\",\"barcode\":\"" + barcode + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.channel").value("DESK"));

        // The same copy can't go out twice
        mvc.perform(auth(post("/api/desk/checkout"), librarian).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cardNumber\":\"" + me.get("cardNumber").asText() + "\",\"barcode\":\"" + barcode + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void aReturnedCopyGoesToTheFirstMemberInTheHoldsQueue() throws Exception {
        String librarian = login(LIBRARIAN);
        long sapiens = bookId("Sapiens");
        String kabirsCopy = firstCopyBorrowedBy(librarian, sapiens, "Kabir Singh");

        // Rahul is first in line, so the copy is set aside for him
        mvc.perform(auth(post("/api/desk/checkin"), librarian).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"barcode\":\"" + kabirsCopy + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.holdFor.memberName").value("Rahul Sharma"));

        // Anita, second in line, can't take it
        mvc.perform(auth(post("/api/me/loans"), login(ANITA)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"bookId\":" + sapiens + "}")).andExpect(status().isBadRequest());

        // Rahul borrows it and his hold is fulfilled
        String rahul = login(RAHUL);
        mvc.perform(auth(post("/api/me/loans"), rahul).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bookId\":" + sapiens + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.barcode").value(kabirsCopy));
        JsonNode holds = body(mvc.perform(auth(get("/api/me/holds"), rahul)));
        boolean fulfilled = false;
        for (JsonNode h : holds) {
            fulfilled |= h.get("bookTitle").asText().equals("Sapiens") && h.get("status").asText().equals("FULFILLED");
        }
        assertThat(fulfilled).isTrue();
    }

    @Test
    void holdsAreOnlyForTitlesWithNoCopyOnTheShelf() throws Exception {
        mvc.perform(auth(post("/api/me/holds"), login(ANANYA)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bookId\":" + bookId("Wings of Fire") + "}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("on the shelf")));
    }

    @Test
    void renewalsStopAtTheLimitAndForOtherPeoplesLoans() throws Exception {
        String rahul = login(RAHUL);
        long loanId = openLoanId(rahul, "Effective Java");

        // Another member can't renew Rahul's loan
        mvc.perform(auth(post("/api/me/loans/" + loanId + "/renew"), login(ANANYA))).andExpect(status().isForbidden());

        // Students get one renewal
        mvc.perform(auth(post("/api/me/loans/" + loanId + "/renew"), rahul))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.renewalCount").value(1));
        mvc.perform(auth(post("/api/me/loans/" + loanId + "/renew"), rahul))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("No renewals left")));
    }

    @Test
    void aTitleOthersAreWaitingForCannotBeRenewed() throws Exception {
        // Ananya has a copy of Sapiens; Anita is still queued for it whatever order the tests run in
        String ananya = login(ANANYA);
        mvc.perform(auth(post("/api/me/loans/" + openLoanId(ananya, "Sapiens") + "/renew"), ananya))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("waiting")));
    }

    @Test
    void aBlockedMemberSeesEveryReasonAndCannotBorrow() throws Exception {
        String priya = login(PRIYA);
        mvc.perform(auth(get("/api/me/dashboard"), priya))
                .andExpect(jsonPath("$.eligibility.canBorrow").value(false))
                .andExpect(jsonPath("$.eligibility.blocks.length()").value(2));
        mvc.perform(auth(post("/api/me/loans"), priya).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bookId\":" + bookId("Head First Java") + "}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("overdue")));
    }

    // ------------------------------------------------------------------ fines & reports

    @Test
    void finesArePaidAtTheDeskAndCannotBeOverpaid() throws Exception {
        String librarian = login(LIBRARIAN);
        long fineId = -1;
        for (JsonNode f : body(mvc.perform(auth(get("/api/fines?status=open"), librarian)))) {
            if (f.get("memberName").asText().equals("Kabir Singh")) {
                fineId = f.get("id").asLong();
            }
        }
        assertThat(fineId).isPositive();

        mvc.perform(auth(post("/api/fines/" + fineId + "/payments"), librarian).contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\":1000}")).andExpect(status().isBadRequest());
        mvc.perform(auth(post("/api/fines/" + fineId + "/payments"), librarian).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":45,\"note\":\"UPI\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.settled").value(true))
                .andExpect(jsonPath("$.transactions[0].staffName").value("Meera Nair"));
    }

    @Test
    void overdueReportDownloadsAsCsv() throws Exception {
        mvc.perform(auth(get("/api/reports/overdue.csv"), login(LIBRARIAN)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/csv"))
                .andExpect(content().string(containsString("Clean Code")));
    }

    @Test
    void cancellingAHoldRemovesItFromTheQueue() throws Exception {
        String ananya = login(ANANYA);
        long holdId = -1;
        for (JsonNode h : body(mvc.perform(auth(get("/api/me/holds"), ananya)))) {
            if (h.get("status").asText().equals("READY")) {
                holdId = h.get("id").asLong();
            }
        }
        assertThat(holdId).isPositive();
        mvc.perform(auth(delete("/api/me/holds/" + holdId), login(RAHUL))).andExpect(status().isForbidden());
        mvc.perform(auth(delete("/api/me/holds/" + holdId), ananya))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    // ------------------------------------------------------------------ helpers

    private ResultActions register(String name) throws Exception {
        String email = name.toLowerCase().replaceAll("[^a-z0-9]", ".") + "." + UUID.randomUUID().toString().substring(0, 8) + "@test.com";
        return mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"" + name + "\",\"email\":\"" + email + "\",\"password\":\"secret123\"}"));
    }

    private String login(String credentials) throws Exception {
        String[] parts = credentials.split(":");
        return token(mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + parts[0] + "\",\"password\":\"" + parts[1] + "\"}")).andExpect(status().isOk()));
    }

    private String token(ResultActions result) throws Exception {
        return body(result).get("token").asText();
    }

    private JsonNode body(ResultActions result) throws Exception {
        return json.readTree(result.andReturn().getResponse().getContentAsString());
    }

    private static MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder request, String token) {
        return request.header("Authorization", "Bearer " + token);
    }

    private long bookId(String title) throws Exception {
        JsonNode page = body(mvc.perform(auth(get("/api/books").param("q", title).param("size", "50"), login(ADMIN))));
        for (JsonNode b : page.get("content")) {
            if (b.get("title").asText().equals(title)) {
                return b.get("id").asLong();
            }
        }
        throw new AssertionError("No book " + title);
    }

    private String firstCopy(String staffToken, long bookId, String status) throws Exception {
        for (JsonNode c : body(mvc.perform(auth(get("/api/books/" + bookId + "/copies"), staffToken)))) {
            if (c.get("status").asText().equals(status)) {
                return c.get("barcode").asText();
            }
        }
        throw new AssertionError("No " + status + " copy of book " + bookId);
    }

    private String firstCopyBorrowedBy(String staffToken, long bookId, String member) throws Exception {
        for (JsonNode c : body(mvc.perform(auth(get("/api/books/" + bookId + "/copies"), staffToken)))) {
            if (member.equals(c.path("borrowerName").asText())) {
                return c.get("barcode").asText();
            }
        }
        throw new AssertionError(member + " has no copy of book " + bookId);
    }

    private long openLoanId(String memberToken, String title) throws Exception {
        for (JsonNode l : body(mvc.perform(auth(get("/api/me/loans"), memberToken)))) {
            if (l.get("bookTitle").asText().equals(title) && l.get("returnDate").isNull()) {
                return l.get("id").asLong();
            }
        }
        throw new AssertionError("No open loan for " + title);
    }
}
