import axiosInterface from "./axiosInterface";

/**
 * Get infos about badges
 */
async function getBadgesAPI(accessToken, memberId) {
  const response = await axiosInterface(
    "GET",
    "api/v/member/badge",
    {},
    {
      Authorization: `Baerer ${accessToken}`,
    },
    { memberId: memberId }
  );

  if (response.status === 200) {
    return response;
  }

  return response.response;
}

export default getBadgesAPI;
