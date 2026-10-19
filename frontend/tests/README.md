# Generation feedback browser regression checks

Run the normal Vite dev server, then open `/tests/generation-feedback.html`.
This isolated harness renders the actual generation page and dialog with mocked API responses.
It never calls Gemini or the backend, stores credentials, or writes database rows.
The public test material button emulates selecting a harmless text file; it does not test an OS file chooser or PDF parsing.
This HTML is not an application route or production build entry.

## Checks

1. Enter a topic, attach the public test material, choose one Bloom level and check consent.
   Submit while scrolled to the bottom. The success dialog must be centered in the current viewport,
   show the correct count and pending-review status, and receive keyboard focus.
2. Confirm background scrolling is locked. Tab/Shift+Tab must stay in the dialog.
   Continue, the close button and Escape must dismiss it, unlock scrolling and restore focus.
3. Submit again, then choose **Xem câu hỏi vừa tạo**. The test destination must be
   `/lecturer/questions?status=PENDING_REVIEW&curr=12001`.
4. Select three-question success and confirm the count is 3.
5. Select delayed success. Confirm the form/button are disabled while waiting and repeated submit
   does not increase the request count. Complete the request and verify one success dialog.
6. Test provider error, timeout, empty response and rejected request. No success dialog may appear.
   The alert must be visible near the submit button and must receive focus. Timeout text must tell
   the user to inspect the question list before retrying, without claiming the operation was cancelled.
7. Test no selected Bloom levels and missing consent. There must be no API request.
8. Repeat success and dismissal at a narrow viewport; dialog content and buttons must fit or scroll
   within the viewport. Verify the topic and selected material remain available after dismissal.

Build check: `npm run build`. Lint check: `npm run lint` (the baseline has unrelated existing warnings).
Real material extraction, provider calls and database persistence are separate backend/integration tests.
