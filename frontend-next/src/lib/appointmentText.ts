export const APPOINTMENT_TEXT = {
  common: {
    today: "\uC624\uB298",
    loading: "\uCC98\uB9AC \uC911...",
    placePreparing: "\uC7A5\uC18C \uC120\uD0DD \uC900\uBE44 \uC911"
  },
  quick: {
    label: "\uC57D\uC18D",
    open: "\uC7A1\uAE30",
    openMenu: "\uC57D\uC18D \uC7A1\uAE30",
    completePrefix: "\uAC70\uB798",
    completeSuffix: "\uC644\uB8CC",
    placePrefix: "\uC7A5\uC18C",
    placeSuffix: "\uD655\uC778"
  },
  composer: {
    titleSuffix: "\uC640 \uC57D\uC18D",
    date: "\uB0A0\uC9DC",
    time: "\uC2DC\uAC04",
    place: "\uC7A5\uC18C",
    reminder: "\uC57D\uC18D \uC54C\uB9BC",
    cancel: "\uCDE8\uC18C",
    submit: "\uC644\uB8CC"
  },
  viewer: {
    titleSuffix: "\uC640 \uC57D\uC18D",
    date: "\uB0A0\uC9DC",
    time: "\uC2DC\uAC04",
    place: "\uC7A5\uC18C",
    reminder: "\uC57D\uC18D \uC54C\uB9BC",
    cancel: "\uC57D\uC18D \uCDE8\uC18C",
    close: "\uB2EB\uAE30"
  },
  reminders: [
    { value: null, label: "\uC5C6\uC74C" },
    { value: 1, label: "1\uBD84 \uC804" },
    { value: 5, label: "5\uBD84 \uC804" },
    { value: 15, label: "15\uBD84 \uC804" },
    { value: 30, label: "30\uBD84 \uC804" },
    { value: 60, label: "1\uC2DC\uAC04 \uC804" },
    { value: 120, label: "2\uC2DC\uAC04 \uC804" }
  ] as const
} as const;

export function formatAppointmentPreviewLabel(value: string) {
  const appointmentDate = new Date(value);
  const now = new Date();

  const sameDay =
    appointmentDate.getFullYear() === now.getFullYear() &&
    appointmentDate.getMonth() === now.getMonth() &&
    appointmentDate.getDate() === now.getDate();

  const time = appointmentDate.toLocaleTimeString("ko-KR", {
    hour: "numeric",
    minute: "2-digit"
  });

  if (sameDay) {
    return `${APPOINTMENT_TEXT.common.today} ${time}`;
  }

  return `${appointmentDate.getMonth() + 1}\uC6D4 ${appointmentDate.getDate()}\uC77C ${time}`;
}
