import App from "@/App";
import { cookies } from "next/headers";
import { Suspense } from "react";

const SELECTED_REGION_COOKIE = "goods-selected-region";

type InitialSelectedRegion = {
  region_id: number;
  dongnm: string;
  verified_at: string | null;
  primary?: boolean;
};

function parseSelectedRegionCookie(value: string | undefined): InitialSelectedRegion | null {
  if (!value) {
    return null;
  }

  try {
    const parsed = JSON.parse(decodeURIComponent(value)) as Partial<InitialSelectedRegion>;
    if (typeof parsed.region_id !== "number" || typeof parsed.dongnm !== "string") {
      return null;
    }

    return {
      region_id: parsed.region_id,
      dongnm: parsed.dongnm,
      verified_at: typeof parsed.verified_at === "string" ? parsed.verified_at : null,
      primary: Boolean(parsed.primary)
    };
  } catch {
    return null;
  }
}

export default async function Page() {
  const cookieStore = await cookies();
  const initialSelectedRegion = parseSelectedRegionCookie(cookieStore.get(SELECTED_REGION_COOKIE)?.value);

  return (
    <Suspense fallback={null}>
      <App initialSelectedRegion={initialSelectedRegion} />
    </Suspense>
  );
}
