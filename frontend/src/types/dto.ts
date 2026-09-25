// REST shapes, mirroring document 11. Only the shared error format exists so far.
// TODO(US-01): the request and response types of each endpoint in document 11, section 7

/** One validation issue (document 11, section 6.3). */
export type ValidationIssue = {
  path: string;
  code: string;
  message: string;
};

/** RFC 9457 Problem Details with a stable code (document 11, section 6.1, DEC-144). */
export type ProblemDetails = {
  type: string;
  title: string;
  status: number;
  code: string;
  detail: string | null;
  errors: ValidationIssue[];
};
